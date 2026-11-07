package com.appointmentbooking.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
		@NotBlank @Size(min = 2, max = 80) @Pattern(regexp = ".*\\S.*\\S.*") String firstName,
		@NotBlank @Size(min = 2, max = 80) @Pattern(regexp = ".*\\S.*\\S.*") String lastName,
		@NotBlank @Size(max = 30) @Pattern(regexp = "^[0-9+()\\-\\s]{7,30}$") String phone) {
	public UpdateProfileRequest {
		firstName = firstName == null ? null : firstName.trim();
		lastName = lastName == null ? null : lastName.trim();
		phone = phone == null ? null : phone.trim();
	}
}
