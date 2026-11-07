package com.appointmentbooking.appointment.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RescheduleAppointmentRequest(
		@NotNull @FutureOrPresent LocalDate appointmentDate,
		@NotBlank String time) {
}
