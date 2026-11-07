package com.appointmentbooking.appointment.controller;

import java.util.List;
import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.CreateAppointmentRequest;
import com.appointmentbooking.appointment.dto.RescheduleAppointmentRequest;
import com.appointmentbooking.appointment.service.AppointmentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/appointments")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {
	private final AppointmentService appointmentService;

	public AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@GetMapping("/availability")
	public List<String> getAvailability(@RequestParam Long branchId, @RequestParam Long serviceId,
			@RequestParam LocalDate date) {
		return appointmentService.getAvailableTimes(branchId, serviceId, date);
	}

	@GetMapping("/{appointmentId}/availability")
	public List<String> getRescheduleAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable Long appointmentId,
			@RequestParam LocalDate date) {
		return appointmentService.getRescheduleAvailableTimes(customerId(jwt), appointmentId, date);
	}

	@PatchMapping("/{appointmentId}/reschedule")
	public AppointmentResponse reschedule(@AuthenticationPrincipal Jwt jwt, @PathVariable Long appointmentId,
			@Valid @RequestBody RescheduleAppointmentRequest request) {
		return appointmentService.reschedule(customerId(jwt), appointmentId, request);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AppointmentResponse create(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody CreateAppointmentRequest request) {
		return appointmentService.create(customerId(jwt), request);
	}

	@GetMapping
	public List<AppointmentResponse> getMyAppointments(@AuthenticationPrincipal Jwt jwt) {
		return appointmentService.getForCustomer(customerId(jwt));
	}

	@PatchMapping("/{appointmentId}/cancel")
	public AppointmentResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long appointmentId) {
		return appointmentService.cancel(customerId(jwt), appointmentId);
	}

	private Long customerId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}
}
