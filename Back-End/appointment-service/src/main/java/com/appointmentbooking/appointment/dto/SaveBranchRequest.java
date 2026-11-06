package com.appointmentbooking.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveBranchRequest(
		@NotBlank @Size(max = 120) String name,
		boolean active) {
}
