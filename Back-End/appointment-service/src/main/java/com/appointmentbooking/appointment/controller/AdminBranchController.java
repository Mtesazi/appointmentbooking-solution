package com.appointmentbooking.appointment.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.appointmentbooking.appointment.dto.AdminBranchResponse;
import com.appointmentbooking.appointment.dto.SaveBranchRequest;
import com.appointmentbooking.appointment.dto.BranchHoursRequest;
import com.appointmentbooking.appointment.service.AdminBranchService;
import com.appointmentbooking.appointment.service.BranchHoursService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/branches")
@SecurityRequirement(name = "bearerAuth")
public class AdminBranchController {
	private final AdminBranchService adminBranchService;
	private final BranchHoursService hoursService;

	public AdminBranchController(AdminBranchService adminBranchService, BranchHoursService hoursService) {
		this.adminBranchService = adminBranchService;
		this.hoursService = hoursService;
	}

	@GetMapping("/{id}/hours")
	public List<BranchHoursRequest.Day> getHours(@PathVariable Long id) {
		return hoursService.getSchedule(id);
	}

	@PutMapping("/{id}/hours")
	public List<BranchHoursRequest.Day> updateHours(@PathVariable Long id, @Valid @RequestBody BranchHoursRequest request) {
		return hoursService.updateSchedule(id, request);
	}

	@GetMapping
	public List<AdminBranchResponse> getBranches() {
		return adminBranchService.getBranches();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AdminBranchResponse createBranch(@Valid @RequestBody SaveBranchRequest request) {
		return adminBranchService.createBranch(request);
	}

	@PutMapping("/{id}")
	public AdminBranchResponse updateBranch(@PathVariable Long id,
			@Valid @RequestBody SaveBranchRequest request) {
		return adminBranchService.updateBranch(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deactivateBranch(@PathVariable Long id) {
		adminBranchService.deactivateBranch(id);
	}
}
