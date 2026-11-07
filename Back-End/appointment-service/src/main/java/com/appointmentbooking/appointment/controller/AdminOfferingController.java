package com.appointmentbooking.appointment.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.appointmentbooking.appointment.dto.AdminServiceResponse;
import com.appointmentbooking.appointment.dto.SaveServiceRequest;
import com.appointmentbooking.appointment.service.AdminOfferingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/services")
@SecurityRequirement(name = "bearerAuth")
public class AdminOfferingController {
	private final AdminOfferingService service;

	public AdminOfferingController(AdminOfferingService service) {
		this.service = service;
	}

	@GetMapping
	public List<AdminServiceResponse> getServices() {
		return service.getServices();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AdminServiceResponse create(@Valid @RequestBody SaveServiceRequest request) {
		return service.create(request);
	}

	@PutMapping("/{id}")
	public AdminServiceResponse update(@PathVariable Long id, @Valid @RequestBody SaveServiceRequest request) {
		return service.update(id, request);
	}
}
