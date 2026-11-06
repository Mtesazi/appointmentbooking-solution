package com.appointmentbooking.appointment.mapper;

import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.appointmentbooking.appointment.dto.AdminAppointmentResponse;
import com.appointmentbooking.appointment.dto.AdminAppointmentSummary;

@Component
public class AdminAppointmentMapper {
	private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a");

	public AdminAppointmentResponse toResponse(AdminAppointmentSummary summary) {
		return new AdminAppointmentResponse(summary.id(), summary.customerName(), summary.customerEmail(),
				summary.service(), summary.branch(), summary.date(), summary.startTime().format(DISPLAY_TIME),
				summary.status());
	}
}
