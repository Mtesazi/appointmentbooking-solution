package com.appointmentbooking.appointment.service;

import com.appointmentbooking.appointment.dto.DashboardResponse;

public interface DashboardService {
	DashboardResponse getDashboard(Long customerId);
}
