package com.appointmentbooking.appointment.dto;

import java.time.LocalDate;

import com.appointmentbooking.appointment.entity.Appointment;

public record AdminAppointmentResponse(
		Long id,
		String customerName,
		String customerEmail,
		String service,
		String branch,
		LocalDate date,
		String time,
		Appointment.Status status) {
}
