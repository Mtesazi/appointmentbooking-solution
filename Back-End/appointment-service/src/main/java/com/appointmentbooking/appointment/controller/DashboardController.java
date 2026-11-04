package com.appointmentbooking.appointment.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.DashboardResponse;
import com.appointmentbooking.appointment.service.DashboardService;

@RestController
@RequestMapping("/api/customer")
public class DashboardController {
	private final DashboardService dashboardService;

	public DashboardController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@GetMapping("/dashboard")
	public DashboardResponse getDashboard(@AuthenticationPrincipal Jwt jwt) {
		return dashboardService.getDashboard(Long.valueOf(jwt.getSubject()));
	}
}
