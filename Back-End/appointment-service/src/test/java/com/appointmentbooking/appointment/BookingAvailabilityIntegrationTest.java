package com.appointmentbooking.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.appointmentbooking.appointment.exception.ApiExceptionHandler;
import com.appointmentbooking.appointment.repository.CustomerRepository;
import com.appointmentbooking.appointment.service.AppointmentService;
import com.appointmentbooking.appointment.service.TokenService;
import com.appointmentbooking.appointment.service.BranchHoursService;
import com.appointmentbooking.appointment.dto.BranchHoursRequest;
import com.appointmentbooking.appointment.exception.ConflictException;

@EnabledIfEnvironmentVariable(named = "BOOKING_INTEGRATION_TEST", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class BookingAvailabilityIntegrationTest {
	@Autowired private JdbcTemplate jdbc;
	@Autowired private DataSource dataSource;
	@Autowired private AppointmentService service;
	@Autowired private CustomerRepository customers;
	@Autowired private TokenService tokens;
	@Autowired private BranchHoursService hours;
	@LocalServerPort private int port;

	private final LocalDate date = LocalDate.now().plusDays(10);
	private long customerId;
	private long branchId;
	private long otherBranchId;

	@Test
	void adminServicesManageCatalogAndPreserveExistingBookings() throws Exception {
		long adminId = jdbc.queryForObject("""
				INSERT INTO customers (first_name, last_name, email, phone, password_hash, role)
				VALUES ('Test', 'Admin', ?, '1234567', 'hash', 'ADMIN') RETURNING id
				""", Long.class, UUID.randomUUID() + "@example.invalid");
		Long serviceId = null;
		try {
			String adminToken = tokens.createToken(customers.findById(adminId).orElseThrow());
			String customerToken = tokens.createToken(customers.findById(customerId).orElseThrow());
			var client = HttpClient.newHttpClient();
			String base = "http://localhost:" + port + "/api/admin/services";
			String name = "Test " + UUID.randomUUID();
			String body = "{\"name\":\"" + name + "\",\"durationMinutes\":30,\"active\":true}";
			assertThat(serviceRequest(client, base, customerToken, "POST", body).statusCode()).isEqualTo(403);
			assertThat(serviceRequest(client, base, adminToken, "POST",
					"{\"name\":\"Bad\",\"durationMinutes\":0,\"active\":true}").statusCode()).isEqualTo(400);
			assertThat(serviceRequest(client, base, adminToken, "POST", body).statusCode()).isEqualTo(201);
			serviceId = jdbc.queryForObject("SELECT id FROM services WHERE name = ?", Long.class, name);
			String duplicate = "{\"name\":\"" + name.toUpperCase(java.util.Locale.ROOT)
					+ "\",\"durationMinutes\":30,\"active\":true}";
			assertThat(serviceRequest(client, base, adminToken, "POST", duplicate).statusCode()).isEqualTo(409);
			long appointmentId = jdbc.queryForObject("""
					INSERT INTO appointments (customer_id, branch_id, service_id, appointment_date, start_time, end_time, status)
					VALUES (?, ?, ?, ?, '10:00', '10:30', 'CONFIRMED') RETURNING id
					""", Long.class, customerId, branchId, serviceId, date);
			String updated = "{\"name\":\"" + name + "\",\"durationMinutes\":60,\"active\":false}";
			assertThat(serviceRequest(client, base + "/" + serviceId, adminToken, "PUT", updated).statusCode()).isEqualTo(200);
			assertThat(jdbc.queryForObject("SELECT end_time::text FROM appointments WHERE id = ?", String.class, appointmentId))
					.isEqualTo("10:30:00");
			var catalog = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/services"))
					.header("Authorization", "Bearer " + customerToken).GET().build(), HttpResponse.BodyHandlers.ofString());
			assertThat(catalog.body()).doesNotContain(name);
			long savedServiceId = serviceId;
			assertThatThrownBy(() -> service.getAvailableTimes(branchId, savedServiceId, date))
					.isInstanceOf(com.appointmentbooking.appointment.exception.ResourceNotFoundException.class);
			String booking = "{\"branchId\":" + branchId + ",\"serviceId\":" + serviceId
					+ ",\"appointmentDate\":\"" + date + "\",\"time\":\"2:00 PM\"}";
			assertThat(serviceRequest(client, "http://localhost:" + port + "/api/appointments",
					customerToken, "POST", booking).statusCode()).isEqualTo(404);
			assertThat(service.getRescheduleAvailableTimes(customerId, appointmentId, date)).contains("10:00 AM");
			var response = reschedule(client, "http://localhost:" + port + "/api/appointments/" + appointmentId,
					customerToken, date, "11:00 AM");
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(jdbc.queryForObject("SELECT end_time::text FROM appointments WHERE id = ?", String.class, appointmentId))
					.isEqualTo("12:00:00");
			assertThat(serviceRequest(client, base + "/" + serviceId, adminToken, "PUT",
					"{\"name\":\"" + name + "\",\"durationMinutes\":60,\"active\":true}").statusCode()).isEqualTo(200);
			assertThat(service.getAvailableTimes(branchId, serviceId, date)).doesNotContain("11:00 AM");
		} finally {
			jdbc.update("DELETE FROM appointments WHERE customer_id = ?", customerId);
			if (serviceId != null) {
				jdbc.update("DELETE FROM services WHERE id = ?", serviceId);
			}
			jdbc.update("DELETE FROM customers WHERE id = ?", adminId);
		}
	}

	private HttpResponse<String> serviceRequest(HttpClient client, String url, String token, String method, String body)
			throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void openingHoursControlAvailabilityBookingAndExistingAppointments() throws Exception {
		int weekday = date.getDayOfWeek().getValue();
		assertThat(hours.getSchedule(branchId)).hasSize(7).allMatch(day ->
				!day.closed() && day.openTime().equals("09:00") && day.closeTime().equals("17:00"));
		var updated = hours.getSchedule(branchId).stream().map(day -> day.dayOfWeek() == weekday
				? new BranchHoursRequest.Day(weekday, false, "10:15", "12:00") : day).toList();
		hours.updateSchedule(branchId, new BranchHoursRequest(updated));
		assertThat(service.getAvailableTimes(branchId, 1L, date))
				.containsExactly("10:15 AM", "10:45 AM", "11:15 AM");
		var client = HttpClient.newHttpClient();
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		String url = "http://localhost:" + port + "/api/appointments";
		String body = """
				{"branchId":%d,"serviceId":1,"appointmentDate":"%s","time":"9:00 AM"}
				""".formatted(branchId, date);
		var response = client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(400);
		insert(branchId, "10:30", "11:00", "CONFIRMED");
		long appointmentId = jdbc.queryForObject("SELECT id FROM appointments WHERE customer_id = ?", Long.class, customerId);
		assertThat(reschedule(client, url + "/" + appointmentId, token, date, "9:00 AM").statusCode()).isEqualTo(400);
		var closed = updated.stream().map(day -> day.dayOfWeek() == weekday
				? new BranchHoursRequest.Day(weekday, true, null, null) : day).toList();
		assertThatThrownBy(() -> hours.updateSchedule(branchId, new BranchHoursRequest(closed)))
				.isInstanceOf(ConflictException.class);
		assertThat(hours.getSchedule(branchId)).contains(new BranchHoursRequest.Day(weekday, false, "10:15", "12:00"));
		jdbc.update("UPDATE appointments SET status = 'CANCELLED' WHERE customer_id = ?", customerId);
		hours.updateSchedule(branchId, new BranchHoursRequest(closed));
		assertThat(service.getAvailableTimes(branchId, 1L, date)).isEmpty();
		assertThat(client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
				HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
	}

	@Test
	void hoursApiRequiresAdminAndValidWeek() throws Exception {
		long adminId = jdbc.queryForObject("""
				INSERT INTO customers (first_name, last_name, email, phone, password_hash, role)
				VALUES ('Test', 'Admin', ?, '1234567', 'hash', 'ADMIN') RETURNING id
				""", Long.class, UUID.randomUUID() + "@example.invalid");
		try {
			String adminToken = tokens.createToken(customers.findById(adminId).orElseThrow());
			String customerToken = tokens.createToken(customers.findById(customerId).orElseThrow());
			var client = HttpClient.newHttpClient();
			String url = "http://localhost:" + port + "/api/admin/branches/" + branchId + "/hours";
			var get = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + customerToken).GET().build();
			assertThat(client.send(get, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(403);
			var invalid = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + adminToken)
					.header("Content-Type", "application/json")
					.PUT(HttpRequest.BodyPublishers.ofString("{\"days\":[]}")).build();
			assertThat(client.send(invalid, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(400);
			var valid = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + adminToken).GET().build();
			assertThat(client.send(valid, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
			String body = "{\"days\":[" + java.util.stream.IntStream.rangeClosed(1, 7)
					.mapToObj(day -> "{\"dayOfWeek\":" + day + ",\"closed\":false,\"openTime\":\"09:00\",\"closeTime\":\"17:00\"}")
					.collect(java.util.stream.Collectors.joining(",")) + "]}";
			var put = HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + adminToken)
					.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body)).build();
			assertThat(client.send(put, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
		} finally {
			jdbc.update("DELETE FROM customers WHERE id = ?", adminId);
		}
	}

		@Test
		void scheduleUpdateWaitsForConcurrentBookingAndRejectsDisplacement() throws Exception {
			int weekday = date.getDayOfWeek().getValue();
			var closed = hours.getSchedule(branchId).stream().map(day -> day.dayOfWeek() == weekday
					? new BranchHoursRequest.Day(weekday, true, null, null) : day).toList();
			try (Connection connection = dataSource.getConnection(); var executor = Executors.newSingleThreadExecutor()) {
				connection.setAutoCommit(false);
				try (var lock = connection.prepareStatement("SELECT id FROM branches WHERE id = ? FOR SHARE")) {
					lock.setLong(1, branchId);
					lock.executeQuery().close();
				}
				insert(connection, "10:00", "10:30");
				var pending = executor.submit(() -> {
					try {
						hours.updateSchedule(branchId, new BranchHoursRequest(closed));
						return false;
					} catch (ConflictException exception) {
						return true;
					}
				});
				boolean waiting = false;
				try {
					long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
					while (System.nanoTime() < deadline) {
						waiting = Boolean.TRUE.equals(jdbc.queryForObject("""
								SELECT EXISTS (SELECT 1 FROM pg_stat_activity WHERE datname = current_database()
								AND wait_event_type = 'Lock' AND lower(query) LIKE '%from branches%')
								""", Boolean.class));
						if (waiting || pending.isDone()) {
							break;
						}
						Thread.sleep(20);
					}
				} finally {
					connection.commit();
				}
				assertThat(waiting).isTrue();
				assertThat(pending.get(10, TimeUnit.SECONDS)).isTrue();
				assertThat(hours.getSchedule(branchId)).allMatch(day -> !day.closed());
			}
		}

	@BeforeEach
	void setup() {
		String suffix = UUID.randomUUID().toString();
		customerId = jdbc.queryForObject("""
				INSERT INTO customers (first_name, last_name, email, phone, password_hash, role)
				VALUES ('Test', 'Customer', ?, '1234567', 'hash', 'CUSTOMER') RETURNING id
				""", Long.class, suffix + "@example.invalid");
		branchId = jdbc.queryForObject("INSERT INTO branches (name, active) VALUES (?, true) RETURNING id",
				Long.class, "Test " + suffix);
		otherBranchId = jdbc.queryForObject("INSERT INTO branches (name, active) VALUES (?, true) RETURNING id",
				Long.class, "Other " + suffix);
	}

	@Test
	void availabilityEndpointRequiresAuthenticationAndReturnsAvailableSlots() throws Exception {
		insert(branchId, "10:00", "10:30", "CONFIRMED");
		var uri = URI.create("http://localhost:" + port + "/api/appointments/availability?branchId="
				+ branchId + "&serviceId=1&date=" + date);
		var client = HttpClient.newHttpClient();
		var unauthorized = client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
		assertThat(unauthorized.statusCode()).isEqualTo(401);
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		var response = client.send(HttpRequest.newBuilder(uri).header("Authorization", "Bearer " + token)
				.GET().build(), HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("9:00 AM").doesNotContain("10:00 AM");
	}

	@Test
	void bookingEndpointReturnsConflictForOverlappingTime() throws Exception {
		insert(branchId, "10:00", "10:30", "CONFIRMED");
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		String body = """
				{"branchId":%d,"serviceId":1,"appointmentDate":"%s","time":"10:00 AM"}
				""".formatted(branchId, date);
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/appointments"))
				.header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(response.body()).contains("This time is no longer available");
	}

	@Test
	void rescheduleEnforcesOwnershipStatusAndAvailability() throws Exception {
		long id = jdbc.queryForObject("""
				INSERT INTO appointments (customer_id, branch_id, service_id, appointment_date, start_time, end_time, status)
				VALUES (?, ?, 1, ?, '10:00', '10:30', 'CONFIRMED') RETURNING id
				""", Long.class, customerId, branchId, date);
		insert(branchId, "11:00", "11:30", "CONFIRMED");
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		var client = HttpClient.newHttpClient();
		String base = "http://localhost:" + port + "/api/appointments/" + id;
		var availability = client.send(HttpRequest.newBuilder(URI.create(base + "/availability?date=" + date))
				.header("Authorization", "Bearer " + token).GET().build(), HttpResponse.BodyHandlers.ofString());
		assertThat(availability.statusCode()).isEqualTo(200);
		assertThat(availability.body()).contains("10:00 AM").doesNotContain("11:00 AM");
		assertThat(reschedule(client, base, token, date, "11:00 AM").statusCode()).isEqualTo(409);
		assertThat(reschedule(client, base, token, date.minusDays(30), "9:00 AM").statusCode()).isEqualTo(400);

		long otherCustomerId = jdbc.queryForObject("""
				INSERT INTO customers (first_name, last_name, email, phone, password_hash, role)
				VALUES ('Other', 'Customer', ?, '1234567', 'hash', 'CUSTOMER') RETURNING id
				""", Long.class, UUID.randomUUID() + "@example.invalid");
		try {
			String otherToken = tokens.createToken(customers.findById(otherCustomerId).orElseThrow());
			assertThat(reschedule(client, base, otherToken, date, "12:00 PM").statusCode()).isEqualTo(404);
			var otherAvailability = client.send(HttpRequest.newBuilder(URI.create(base + "/availability?date=" + date))
					.header("Authorization", "Bearer " + otherToken).GET().build(), HttpResponse.BodyHandlers.ofString());
			assertThat(otherAvailability.statusCode()).isEqualTo(404);
		} finally {
			jdbc.update("DELETE FROM customers WHERE id = ?", otherCustomerId);
		}

		var success = reschedule(client, base, token, date.plusDays(1), "12:00 PM");
		assertThat(success.statusCode()).isEqualTo(200);
		assertThat(success.body()).contains(date.plusDays(1).toString(), "12:00 PM");
		assertThat(jdbc.queryForObject("SELECT branch_id FROM appointments WHERE id = ?", Long.class, id))
				.isEqualTo(branchId);
		assertThat(jdbc.queryForObject("SELECT service_id FROM appointments WHERE id = ?", Long.class, id))
				.isEqualTo(1L);
		jdbc.update("UPDATE appointments SET status = 'CANCELLED' WHERE id = ?", id);
		assertThat(reschedule(client, base, token, date, "9:00 AM").statusCode()).isEqualTo(409);
		jdbc.update("UPDATE appointments SET status = 'COMPLETED' WHERE id = ?", id);
		assertThat(reschedule(client, base, token, date, "9:00 AM").statusCode()).isEqualTo(409);
		jdbc.update("UPDATE appointments SET status = 'CONFIRMED', appointment_date = ? WHERE id = ?",
				LocalDate.now().minusDays(1), id);
		assertThat(reschedule(client, base, token, date, "9:00 AM").statusCode()).isEqualTo(409);
	}

	@Test
	void rescheduleCannotRaceWithAnotherBooking() throws Exception {
		long id = jdbc.queryForObject("""
				INSERT INTO appointments (customer_id, branch_id, service_id, appointment_date, start_time, end_time, status)
				VALUES (?, ?, 1, ?, '9:00', '9:30', 'CONFIRMED') RETURNING id
				""", Long.class, customerId, branchId, date);
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		String body = """
				{"appointmentDate":"%s","time":"10:00 AM"}
				""".formatted(date);
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/appointments/" + id + "/reschedule"))
				.header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build();
		try (Connection connection = dataSource.getConnection()) {
			connection.setAutoCommit(false);
			insert(connection, "10:00", "10:30");
			var pending = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
			boolean waiting = false;
			try {
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
				while (System.nanoTime() < deadline) {
					waiting = Boolean.TRUE.equals(jdbc.queryForObject("""
							SELECT EXISTS (SELECT 1 FROM pg_stat_activity
							WHERE datname = current_database() AND wait_event_type = 'Lock'
							AND lower(query) LIKE '%update appointments%')
							""", Boolean.class));
					if (waiting || pending.isDone()) {
						break;
					}
					Thread.sleep(20);
				}
			} finally {
				connection.commit();
			}
			assertThat(waiting).isTrue();
			var response = pending.get(10, TimeUnit.SECONDS);
			assertThat(response.statusCode()).isEqualTo(409);
			assertThat(response.body()).contains("This time is no longer available");
		}
		assertThat(jdbc.queryForObject("SELECT start_time::text FROM appointments WHERE id = ?", String.class, id))
				.isEqualTo("09:00:00");
	}

	@Test
	void cancellationEnforcesOwnershipStatusAndScheduledStart() throws Exception {
		long id = jdbc.queryForObject("""
				INSERT INTO appointments (customer_id, branch_id, service_id, appointment_date, start_time, end_time, status)
				VALUES (?, ?, 1, ?, '10:00', '10:30', 'CONFIRMED') RETURNING id
				""", Long.class, customerId, branchId, date);
		var client = HttpClient.newHttpClient();
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		String url = "http://localhost:" + port + "/api/appointments/" + id + "/cancel";
		long otherCustomerId = jdbc.queryForObject("""
				INSERT INTO customers (first_name, last_name, email, phone, password_hash, role)
				VALUES ('Other', 'Customer', ?, '1234567', 'hash', 'CUSTOMER') RETURNING id
				""", Long.class, UUID.randomUUID() + "@example.invalid");
		try {
			String otherToken = tokens.createToken(customers.findById(otherCustomerId).orElseThrow());
			assertThat(cancel(client, url, otherToken).statusCode()).isEqualTo(404);
		} finally {
			jdbc.update("DELETE FROM customers WHERE id = ?", otherCustomerId);
		}
		var success = cancel(client, url, token);
		assertThat(success.statusCode()).isEqualTo(200);
		assertThat(success.body()).contains("CANCELLED");
		assertThat(service.getAvailableTimes(branchId, 1L, date)).contains("10:00 AM");
		assertThat(cancel(client, url, token).statusCode()).isEqualTo(409);
		jdbc.update("UPDATE appointments SET status = 'COMPLETED' WHERE id = ?", id);
		assertThat(cancel(client, url, token).statusCode()).isEqualTo(409);
		jdbc.update("""
				UPDATE appointments SET status = 'CONFIRMED', appointment_date = CURRENT_DATE,
				start_time = localtime - interval '1 minute', end_time = localtime + interval '10 minutes'
				WHERE id = ?
				""", id);
		var started = cancel(client, url, token);
		assertThat(started.statusCode()).isEqualTo(409);
		assertThat(started.body()).contains("have not started");
		assertThat(jdbc.queryForObject("SELECT status FROM appointments WHERE id = ?", String.class, id))
				.isEqualTo("CONFIRMED");
	}

	@Test
	void adminCancellationEnforcesAuthorizationAndDeadline() throws Exception {
		long id = jdbc.queryForObject("""
				INSERT INTO appointments (customer_id, branch_id, service_id, appointment_date, start_time, end_time, status)
				VALUES (?, ?, 1, ?, '10:00', '10:30', 'CONFIRMED') RETURNING id
				""", Long.class, customerId, branchId, date);
		var client = HttpClient.newHttpClient();
		String customerToken = tokens.createToken(customers.findById(customerId).orElseThrow());
		String url = "http://localhost:" + port + "/api/admin/appointments/" + id + "/cancel";
		assertThat(client.send(HttpRequest.newBuilder(URI.create(url))
				.method("PATCH", HttpRequest.BodyPublishers.noBody()).build(),
				HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
		assertThat(cancel(client, url, customerToken).statusCode()).isEqualTo(403);
		long adminId = jdbc.queryForObject("""
				INSERT INTO customers (first_name, last_name, email, phone, password_hash, role)
				VALUES ('Test', 'Admin', ?, '1234567', 'hash', 'ADMIN') RETURNING id
				""", Long.class, UUID.randomUUID() + "@example.invalid");
		try {
			String adminToken = tokens.createToken(customers.findById(adminId).orElseThrow());
			assertThat(cancel(client, url, adminToken).statusCode()).isEqualTo(204);
			assertThat(jdbc.queryForObject("SELECT status FROM appointments WHERE id = ?", String.class, id))
					.isEqualTo("CANCELLED");
			assertThat(service.getAvailableTimes(branchId, 1L, date)).contains("10:00 AM");
			assertThat(cancel(client, url, adminToken).statusCode()).isEqualTo(409);
			jdbc.update("UPDATE appointments SET status = 'COMPLETED' WHERE id = ?", id);
			assertThat(cancel(client, url, adminToken).statusCode()).isEqualTo(409);
			jdbc.update("""
					UPDATE appointments SET status = 'CONFIRMED', appointment_date = CURRENT_DATE,
					start_time = localtime - interval '1 minute', end_time = localtime + interval '10 minutes'
					WHERE id = ?
					""", id);
			assertThat(cancel(client, url, adminToken).statusCode()).isEqualTo(409);
			assertThat(jdbc.queryForObject("SELECT status FROM appointments WHERE id = ?", String.class, id))
					.isEqualTo("CONFIRMED");
			assertThat(cancel(client, "http://localhost:" + port + "/api/admin/appointments/0/cancel", adminToken)
					.statusCode()).isEqualTo(404);
		} finally {
			jdbc.update("DELETE FROM customers WHERE id = ?", adminId);
		}
	}

	@Test
	void customerProfileUpdateRequiresCustomerRoleAndCannotAlterOtherFields() throws Exception {
		var client = HttpClient.newHttpClient();
		String url = "http://localhost:" + port + "/api/customer/profile";
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		String originalEmail = customers.findById(customerId).orElseThrow().getEmail();
		String valid = """
				{"firstName":"  New  ","lastName":"  Name  ","phone":"  +123456789  ",
				"email":"changed@example.invalid","role":"ADMIN"}
				""";
		var noToken = client.send(HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
				.PUT(HttpRequest.BodyPublishers.ofString(valid)).build(), HttpResponse.BodyHandlers.ofString());
		assertThat(noToken.statusCode()).isEqualTo(401);
		assertThat(updateProfile(client, url, token, """
				{"firstName":" A ","lastName":"Name","phone":"1234567"}
				""").statusCode()).isEqualTo(400);
		assertThat(updateProfile(client, url, token, """
				{"firstName":"Valid","lastName":"Name","phone":" 123 "}
				""").statusCode()).isEqualTo(400);
		var updated = updateProfile(client, url, token, valid);
		assertThat(updated.statusCode()).isEqualTo(200);
		assertThat(updated.body()).contains("\"firstName\":\"New\"", "\"lastName\":\"Name\"",
				"\"phone\":\"+123456789\"", "\"role\":\"CUSTOMER\"", originalEmail);
		var customer = customers.findById(customerId).orElseThrow();
		assertThat(customer.getFirstName()).isEqualTo("New");
		assertThat(customer.getEmail()).isEqualTo(originalEmail);
		assertThat(customer.getPasswordHash()).isEqualTo("hash");
		var read = client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token)
				.GET().build(), HttpResponse.BodyHandlers.ofString());
		assertThat(read.statusCode()).isEqualTo(200);
		assertThat(read.body()).contains("\"firstName\":\"New\"");
		long adminId = jdbc.queryForObject("""
				INSERT INTO customers (first_name, last_name, email, phone, password_hash, role)
				VALUES ('Test', 'Admin', ?, '1234567', 'hash', 'ADMIN') RETURNING id
				""", Long.class, UUID.randomUUID() + "@example.invalid");
		try {
			String adminToken = tokens.createToken(customers.findById(adminId).orElseThrow());
			assertThat(updateProfile(client, url, adminToken, valid).statusCode()).isEqualTo(403);
		} finally {
			jdbc.update("DELETE FROM customers WHERE id = ?", adminId);
		}
	}

	private HttpResponse<String> updateProfile(HttpClient client, String url, String token, String body) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(body)).build(),
				HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> cancel(HttpClient client, String url, String token) throws Exception {
		return client.send(HttpRequest.newBuilder(URI.create(url)).header("Authorization", "Bearer " + token)
				.method("PATCH", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> reschedule(HttpClient client, String base, String token, LocalDate newDate, String time)
			throws Exception {
		String body = """
				{"appointmentDate":"%s","time":"%s"}
				""".formatted(newDate, time);
		return client.send(HttpRequest.newBuilder(URI.create(base + "/reschedule"))
				.header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
				.method("PATCH", HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void bookingEndpointReturnsConflictForConcurrentInsert() throws Exception {
		String token = tokens.createToken(customers.findById(customerId).orElseThrow());
		String body = """
				{"branchId":%d,"serviceId":1,"appointmentDate":"%s","time":"10:00 AM"}
				""".formatted(branchId, date);
		var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/appointments"))
				.header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body)).build();
		try (Connection first = dataSource.getConnection()) {
			first.setAutoCommit(false);
			insert(first, "10:00", "10:30");
			var pending = HttpClient.newHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString());
			boolean waiting = false;
			try {
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
				while (System.nanoTime() < deadline) {
					waiting = Boolean.TRUE.equals(jdbc.queryForObject("""
							SELECT EXISTS (SELECT 1 FROM pg_stat_activity
							WHERE datname = current_database() AND wait_event_type = 'Lock'
							AND lower(query) LIKE '%insert into appointments%')
							""", Boolean.class));
					if (waiting || pending.isDone()) {
						break;
					}
					Thread.sleep(20);
				}
			} finally {
				first.commit();
			}
			assertThat(waiting).isTrue();
			var response = pending.get(10, TimeUnit.SECONDS);
			assertThat(response.statusCode()).isEqualTo(409);
			assertThat(response.body()).contains("This time is no longer available");
		}
	}

	@AfterEach
	void cleanup() {
		jdbc.update("DELETE FROM appointments WHERE customer_id = ?", customerId);
		jdbc.update("DELETE FROM branches WHERE id IN (?, ?)", branchId, otherBranchId);
		jdbc.update("DELETE FROM customers WHERE id = ?", customerId);
	}

	@Test
	void availabilityRespectsDurationOverlapAndCancellation() {
		insert(branchId, "10:00", "10:30", "CONFIRMED");
		insert(branchId, "11:00", "11:30", "CANCELLED");
		insert(otherBranchId, "12:00", "12:30", "CONFIRMED");

		assertThat(service.getAvailableTimes(branchId, 3L, date))
				.contains("9:00 AM", "11:00 AM", "12:00 PM", "4:00 PM")
				.doesNotContain("9:30 AM", "10:00 AM");
		assertThat(service.getAvailableTimes(branchId, 1L, LocalDate.now().minusDays(1))).isEmpty();
	}

	@Test
	void overlappingConcurrentTransactionsCannotBothCommit() throws Exception {
		try (Connection first = dataSource.getConnection(); Connection second = dataSource.getConnection();
				var executor = Executors.newSingleThreadExecutor()) {
			first.setAutoCommit(false);
			second.setAutoCommit(false);
			int secondPid;
			try (var query = second.createStatement(); var result = query.executeQuery("SELECT pg_backend_pid()")) {
				result.next();
				secondPid = result.getInt(1);
			}
			insert(first, "10:00", "10:30");
			CountDownLatch started = new CountDownLatch(1);
			var other = executor.submit(() -> {
				started.countDown();
				try {
					insert(second, "10:15", "10:45");
					second.commit();
					return null;
				} catch (SQLException exception) {
					second.rollback();
					return exception;
				}
			});
			boolean waiting = false;
			try {
				assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
				while (System.nanoTime() < deadline) {
					waiting = Boolean.TRUE.equals(jdbc.queryForObject(
							"SELECT wait_event_type = 'Lock' FROM pg_stat_activity WHERE pid = ?",
							Boolean.class, secondPid));
					if (waiting) {
						break;
					}
					Thread.sleep(20);
				}
			} finally {
				first.commit();
			}
			assertThat(waiting).isTrue();
			SQLException conflict = other.get(10, TimeUnit.SECONDS);
			assertNotNull(conflict);
			assertThat(conflict.getSQLState()).isEqualTo("23P01");
			var response = new ApiExceptionHandler().dataConflict(new DataIntegrityViolationException("conflict", conflict));
			assertThat(response.getStatusCode().value()).isEqualTo(409);
			assertThat(response.getBody().message()).contains("no longer available");
		}
		insert(branchId, "10:30", "11:00", "CONFIRMED");
		assertThatThrownBy(() -> insert(branchId, "10:15", "10:45", "CONFIRMED"))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private void insert(long branch, String start, String end, String status) {
		jdbc.update("""
				INSERT INTO appointments (customer_id, branch_id, service_id, appointment_date, start_time, end_time, status)
				VALUES (?, ?, 1, ?, ?::time, ?::time, ?)
				""", customerId, branch, date, start, end, status);
	}

	private void insert(Connection connection, String start, String end) throws SQLException {
		try (var statement = connection.prepareStatement("""
				INSERT INTO appointments (customer_id, branch_id, service_id, appointment_date, start_time, end_time, status)
				VALUES (?, ?, 1, ?, ?::time, ?::time, 'CONFIRMED')
				""")) {
			statement.setLong(1, customerId);
			statement.setLong(2, branchId);
			statement.setObject(3, date);
			statement.setString(4, start);
			statement.setString(5, end);
			statement.executeUpdate();
		}
	}
}
