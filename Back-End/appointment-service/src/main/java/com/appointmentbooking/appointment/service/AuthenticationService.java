package com.appointmentbooking.appointment.service;

import com.appointmentbooking.appointment.dto.AuthResponse;
import com.appointmentbooking.appointment.dto.CustomerResponse;
import com.appointmentbooking.appointment.dto.LoginRequest;
import com.appointmentbooking.appointment.dto.RegisterRequest;
import com.appointmentbooking.appointment.dto.UpdateProfileRequest;

public interface AuthenticationService {
	AuthResponse register(RegisterRequest request);

	AuthResponse login(LoginRequest request);

	CustomerResponse getCustomer(Long customerId);

	CustomerResponse updateProfile(Long customerId, UpdateProfileRequest request);
}
