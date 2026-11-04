package com.appointmentbooking.appointment.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.BranchResponse;
import com.appointmentbooking.appointment.dto.ServiceResponse;
import com.appointmentbooking.appointment.service.CatalogService;

@RestController
@RequestMapping("/api")
public class CatalogController {
	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/branches")
	public List<BranchResponse> getBranches() {
		return catalogService.getBranches();
	}

	@GetMapping("/services")
	public List<ServiceResponse> getServices() {
		return catalogService.getServices();
	}
}
