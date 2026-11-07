package com.appointmentbooking.appointment.service;

import java.util.List;
import java.time.LocalDate;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.CreateAppointmentRequest;
import com.appointmentbooking.appointment.dto.RescheduleAppointmentRequest;

public interface AppointmentService {
	List<String> getAvailableTimes(Long branchId, Long serviceId, LocalDate date);
	List<String> getRescheduleAvailableTimes(Long customerId, Long appointmentId, LocalDate date);
	AppointmentResponse reschedule(Long customerId, Long appointmentId, RescheduleAppointmentRequest request);
	AppointmentResponse create(Long customerId, CreateAppointmentRequest request);

	List<AppointmentResponse> getForCustomer(Long customerId);

	AppointmentResponse cancel(Long customerId, Long appointmentId);

	void cancelAsAdmin(Long appointmentId);
}
