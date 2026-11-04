package com.appointmentbooking.appointment.dto;

import java.time.LocalDate;

import com.appointmentbooking.appointment.entity.Appointment;

public record AppointmentResponse(
		Long id,
		String branch,
		String service,
		LocalDate date,
		String time,
		Appointment.Status status,
		String notes) {
}
