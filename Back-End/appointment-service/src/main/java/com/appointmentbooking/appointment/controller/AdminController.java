package com.appointmentbooking.appointment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.AdminAppointmentResponse;
import com.appointmentbooking.appointment.dto.AdminDashboardResponse;
import com.appointmentbooking.appointment.service.AdminService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {
	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	@GetMapping("/dashboard")
	public AdminDashboardResponse getDashboard() {
		return adminService.getDashboard();
	}

	@GetMapping("/appointments")
	public List<AdminAppointmentResponse> getAppointments() {
		return adminService.getAppointments();
	}
}
