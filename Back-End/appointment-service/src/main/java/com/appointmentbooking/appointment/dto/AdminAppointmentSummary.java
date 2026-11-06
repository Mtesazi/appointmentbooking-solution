package com.appointmentbooking.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.appointmentbooking.appointment.entity.Appointment;

public record AdminAppointmentSummary(
		Long id,
		String customerName,
		String customerEmail,
		String service,
		String branch,
		LocalDate date,
		LocalTime startTime,
		Appointment.Status status) {
}
