package com.appointmentbooking.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class CorsIntegrationTest {
	private static final AtomicInteger forwardedRequests = new AtomicInteger();
	private static final AtomicReference<String> forwardedOrigin = new AtomicReference<>();
	private static final AtomicReference<String> forwardedAuthorization = new AtomicReference<>();
	private static final HttpServer backend = createBackend();
	private final HttpClient client = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("APPOINTMENT_SERVICE_URI", () -> "http://localhost:" + backend.getAddress().getPort());
	}

	@AfterAll
	static void stopBackend() {
		backend.stop(0);
	}

	@ParameterizedTest
	@ValueSource(strings = {"http://localhost:4200", "http://127.0.0.1:4200",
			"http://localhost:4201", "http://127.0.0.1:4201"})
	void preflightAllowsAngularWithoutForwardingToBackend(String origin) throws Exception {
		int before = forwardedRequests.get();
		HttpResponse<String> response = client.send(request("/api/auth/register")
				.header("Origin", origin)
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type, authorization")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
				HttpResponse.BodyHandlers.ofString());

		assertEquals(200, response.statusCode());
		assertEquals(List.of(origin), response.headers().allValues("Access-Control-Allow-Origin"));
		assertTrue(response.headers().firstValue("Access-Control-Allow-Methods").orElseThrow().contains("POST"));
		assertTrue(response.headers().firstValue("Access-Control-Allow-Headers").orElseThrow()
				.toLowerCase().contains("authorization"));
		assertEquals(before, forwardedRequests.get());
	}

	@ParameterizedTest
	@ValueSource(strings = {"http://localhost:4200", "http://127.0.0.1:4200",
			"http://localhost:4201", "http://127.0.0.1:4201"})
	void actualRequestHasOneCorsHeaderAndDoesNotForwardOrigin(String origin) throws Exception {
		HttpResponse<String> response = client.send(request("/api/auth/login")
				.header("Origin", origin)
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer test-token")
				.POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
				HttpResponse.BodyHandlers.ofString());

		assertEquals(400, response.statusCode());
		assertEquals(List.of(origin), response.headers().allValues("Access-Control-Allow-Origin"));
		assertNull(forwardedOrigin.get());
		assertEquals("Bearer test-token", forwardedAuthorization.get());
	}

	@Test
	void rejectsDisallowedHeadersWithoutForwarding() throws Exception {
		int before = forwardedRequests.get();
		HttpResponse<String> response = client.send(request("/api/auth/login")
				.header("Origin", "http://localhost:4200")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "x-disallowed")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
				HttpResponse.BodyHandlers.ofString());

		assertEquals(403, response.statusCode());
		assertEquals(before, forwardedRequests.get());
	}

	@Test
	void rejectsDisallowedOrigin() throws Exception {
		int before = forwardedRequests.get();
		HttpResponse<String> response = client.send(request("/api/auth/register")
				.header("Origin", "http://example.com")
				.header("Access-Control-Request-Method", "POST")
				.method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(),
				HttpResponse.BodyHandlers.ofString());

		assertEquals(403, response.statusCode());
		assertEquals(List.of(), response.headers().allValues("Access-Control-Allow-Origin"));
		assertEquals(before, forwardedRequests.get());
	}

	private HttpRequest.Builder request(String path) {
		return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
	}

	private static HttpServer createBackend() {
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
			server.createContext("/", exchange -> {
				forwardedRequests.incrementAndGet();
				String origin = exchange.getRequestHeaders().getFirst("Origin");
				forwardedOrigin.set(origin);
				forwardedAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
				if (origin != null) {
					exchange.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
				}
				exchange.sendResponseHeaders(400, -1);
				exchange.close();
			});
			server.start();
			return server;
		} catch (IOException exception) {
			throw new ExceptionInInitializerError(exception);
		}
	}
}
