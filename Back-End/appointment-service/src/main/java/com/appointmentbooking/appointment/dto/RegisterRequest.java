package com.appointmentbooking.appointment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Size(min = 2, max = 80) String firstName,
		@NotBlank @Size(min = 2, max = 80) String lastName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Pattern(regexp = "^[0-9+()\\-\\s]{7,30}$") String phone,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
