package com.appointmentbooking.appointment.service;

import java.util.List;

import com.appointmentbooking.appointment.dto.AdminAppointmentResponse;
import com.appointmentbooking.appointment.dto.AdminDashboardResponse;

public interface AdminService {
	AdminDashboardResponse getDashboard();

	List<AdminAppointmentResponse> getAppointments();
}
