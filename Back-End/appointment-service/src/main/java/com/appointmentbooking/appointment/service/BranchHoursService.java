package com.appointmentbooking.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.BranchHoursRequest;
import com.appointmentbooking.appointment.exception.BadRequestException;
import com.appointmentbooking.appointment.exception.ConflictException;
import com.appointmentbooking.appointment.exception.ResourceNotFoundException;
import com.appointmentbooking.appointment.repository.BranchRepository;

@Service
public class BranchHoursService {
	public record Hours(int dayOfWeek, LocalTime openTime, LocalTime closeTime) {
		public boolean closed() {
			return openTime == null;
		}
	}

	private final JdbcTemplate jdbc;
	private final BranchRepository branches;

	public BranchHoursService(JdbcTemplate jdbc, BranchRepository branches) {
		this.jdbc = jdbc;
		this.branches = branches;
	}

	@Transactional(readOnly = true)
	public List<BranchHoursRequest.Day> getSchedule(Long branchId) {
		if (!branches.existsById(branchId)) {
			throw new ResourceNotFoundException("Branch not found");
		}
		List<Hours> hours = readHours(branchId);
		return hours.stream().map(h -> new BranchHoursRequest.Day(h.dayOfWeek(), h.closed(),
				h.closed() ? null : h.openTime().format(DateTimeFormatter.ofPattern("HH:mm")),
				h.closed() ? null : h.closeTime().format(DateTimeFormatter.ofPattern("HH:mm")))).toList();
	}

	public Hours hoursFor(Long branchId, LocalDate date) {
		return readHours(branchId).stream().filter(h -> h.dayOfWeek() == date.getDayOfWeek().getValue())
				.findFirst().orElseThrow(() -> new IllegalStateException("Branch schedule is incomplete"));
	}

	@Transactional
	public List<BranchHoursRequest.Day> updateSchedule(Long branchId, BranchHoursRequest request) {
		branches.findByIdForUpdate(branchId).orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		Set<Integer> days = new HashSet<>();
		List<Hours> proposed = request.days().stream().map(day -> {
			if (day.dayOfWeek() < 1 || day.dayOfWeek() > 7 || !days.add(day.dayOfWeek())) {
				throw new BadRequestException("Each day of the week must appear exactly once");
			}
			if (day.closed()) {
				if (day.openTime() != null || day.closeTime() != null) {
					throw new BadRequestException("Closed days cannot have opening or closing times");
				}
				return new Hours(day.dayOfWeek(), null, null);
			}
			LocalTime open = parseTime(day.openTime());
			LocalTime close = parseTime(day.closeTime());
			if (!open.isBefore(close)) {
				throw new BadRequestException("Closing time must be after opening time on the same day");
			}
			return new Hours(day.dayOfWeek(), open, close);
		}).toList();
		LocalDateTime now = LocalDateTime.now();
		for (Hours day : proposed) {
			Boolean conflict = jdbc.queryForObject("""
					SELECT EXISTS (SELECT 1 FROM appointments
					WHERE branch_id = ? AND status = 'CONFIRMED'
					AND EXTRACT(ISODOW FROM appointment_date) = ?
					AND appointment_date + start_time > ?
					AND (? OR start_time < ? OR end_time > ?))
					""", Boolean.class, branchId, day.dayOfWeek(), now, day.closed(), day.openTime(), day.closeTime());
			if (Boolean.TRUE.equals(conflict)) {
				throw new ConflictException("The schedule conflicts with existing future appointments; reschedule or cancel them first");
			}
		}
		for (Hours day : proposed) {
			int updated = jdbc.update("""
					UPDATE branch_opening_hours SET open_time = ?, close_time = ?
					WHERE branch_id = ? AND day_of_week = ?
					""", day.openTime(), day.closeTime(), branchId, day.dayOfWeek());
			if (updated != 1) {
				throw new IllegalStateException("Branch schedule is incomplete");
			}
		}
		return getSchedule(branchId);
	}

	private List<Hours> readHours(Long branchId) {
		List<Hours> result = jdbc.query("""
				SELECT day_of_week, open_time, close_time FROM branch_opening_hours
				WHERE branch_id = ? ORDER BY day_of_week
				""", (rs, row) -> new Hours(rs.getInt("day_of_week"),
				rs.getObject("open_time", LocalTime.class), rs.getObject("close_time", LocalTime.class)), branchId);
		if (result.size() != 7) {
			throw new IllegalStateException("Branch schedule is incomplete");
		}
		return result;
	}

	private LocalTime parseTime(String value) {
		if (value == null || !value.matches("\\d{2}:\\d{2}")) {
			throw new BadRequestException("Opening and closing times must use HH:mm");
		}
		try {
			return LocalTime.parse(value);
		} catch (DateTimeParseException exception) {
			throw new BadRequestException("Opening and closing times must use HH:mm");
		}
	}
}
