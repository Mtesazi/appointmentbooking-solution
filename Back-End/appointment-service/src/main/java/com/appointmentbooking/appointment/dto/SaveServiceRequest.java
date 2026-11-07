package com.appointmentbooking.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveServiceRequest(@NotBlank @Size(max = 120) String name,
		@NotNull @Min(1) @Max(1440) Integer durationMinutes, @NotNull Boolean active) {
	public SaveServiceRequest {
		name = name == null ? null : name.trim();
	}
}
