package com.appointmentbooking.appointment.service;

import java.util.List;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.CreateAppointmentRequest;

public interface AppointmentService {
	AppointmentResponse create(Long customerId, CreateAppointmentRequest request);

	List<AppointmentResponse> getForCustomer(Long customerId);

	AppointmentResponse cancel(Long customerId, Long appointmentId);
}
