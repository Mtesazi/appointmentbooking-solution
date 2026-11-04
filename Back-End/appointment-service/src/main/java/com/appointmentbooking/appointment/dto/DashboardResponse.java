package com.appointmentbooking.appointment.dto;

import java.util.List;

public record DashboardResponse(
		String customerName,
		long upcomingAppointments,
		long completedAppointments,
		long cancelledAppointments,
		AppointmentResponse nextAppointment,
		List<AppointmentResponse> recentAppointments) {
}
