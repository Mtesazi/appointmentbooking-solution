package com.appointmentbooking.appointment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.AuthResponse;
import com.appointmentbooking.appointment.dto.CustomerResponse;
import com.appointmentbooking.appointment.dto.LoginRequest;
import com.appointmentbooking.appointment.dto.RegisterRequest;
import com.appointmentbooking.appointment.service.AuthenticationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
	private final AuthenticationService authenticationService;

	public AuthenticationController(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
		return authenticationService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		return authenticationService.login(request);
	}

	@GetMapping("/me")
	public CustomerResponse getCurrentCustomer(@AuthenticationPrincipal Jwt jwt) {
		return authenticationService.getCustomer(Long.valueOf(jwt.getSubject()));
	}
}
