package com.appointmentbooking.appointment.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.appointmentbooking.appointment.entity.Appointment;

public record AppointmentSummary(
		Long id,
		String branch,
		String service,
		LocalDate date,
		LocalTime startTime,
		Appointment.Status status,
		String notes) {
}
