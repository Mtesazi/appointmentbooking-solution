package com.appointmentbooking.appointment.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAppointmentRequest(
		@NotNull Long branchId,
		@NotNull Long serviceId,
		@NotNull @FutureOrPresent LocalDate appointmentDate,
		@NotBlank String time,
		@Size(max = 2000) String notes) {
}
