package com.appointmentbooking.appointment.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.CustomerResponse;
import com.appointmentbooking.appointment.dto.UpdateProfileRequest;
import com.appointmentbooking.appointment.service.AuthenticationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customer/profile")
@SecurityRequirement(name = "bearerAuth")
public class CustomerProfileController {
	private final AuthenticationService authenticationService;

	public CustomerProfileController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@GetMapping
	public CustomerResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
		return authenticationService.getCustomer(Long.valueOf(jwt.getSubject()));
	}

	@PutMapping
	public CustomerResponse updateProfile(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
		return authenticationService.updateProfile(Long.valueOf(jwt.getSubject()), request);
	}
}
