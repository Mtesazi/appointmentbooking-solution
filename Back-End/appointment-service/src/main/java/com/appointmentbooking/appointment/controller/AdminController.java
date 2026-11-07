package com.appointmentbooking.appointment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.AdminAppointmentResponse;
import com.appointmentbooking.appointment.dto.AdminDashboardResponse;
import com.appointmentbooking.appointment.service.AdminService;
import com.appointmentbooking.appointment.service.AppointmentService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
	private final AdminService adminService;
	private final AppointmentService appointmentService;

	public AdminController(AdminService adminService, AppointmentService appointmentService) {
		this.adminService = adminService;
		this.appointmentService = appointmentService;
	}

	@GetMapping("/dashboard")
	public AdminDashboardResponse getDashboard() {
		return adminService.getDashboard();
	}

	@GetMapping("/appointments")
	public List<AdminAppointmentResponse> getAppointments() {
		return adminService.getAppointments();
	}

	@PatchMapping("/appointments/{appointmentId}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancelAppointment(@PathVariable Long appointmentId) {
		appointmentService.cancelAsAdmin(appointmentId);
	}
}
