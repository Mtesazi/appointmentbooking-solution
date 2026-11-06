package com.appointmentbooking.appointment.dto;

import com.appointmentbooking.appointment.entity.Customer;

public record CustomerResponse(
		Long id,
		String firstName,
		String lastName,
		String email,
		String phone,
		Customer.Role role) {
}
