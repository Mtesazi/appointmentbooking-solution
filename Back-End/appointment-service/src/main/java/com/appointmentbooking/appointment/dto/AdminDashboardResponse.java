package com.appointmentbooking.appointment.dto;

public record AdminDashboardResponse(
		long totalAppointments,
		long upcomingAppointments,
		long totalCustomers,
		long activeServices) {
}
