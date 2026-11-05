package com.appointmentbooking.appointment.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.AppointmentSummary;
import com.appointmentbooking.appointment.dto.DashboardResponse;
import com.appointmentbooking.appointment.entity.Appointment;
import com.appointmentbooking.appointment.entity.Customer;
import com.appointmentbooking.appointment.exception.ResourceNotFoundException;
import com.appointmentbooking.appointment.mapper.AppointmentMapper;
import com.appointmentbooking.appointment.repository.AppointmentRepository;
import com.appointmentbooking.appointment.repository.CustomerRepository;
import com.appointmentbooking.appointment.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {
	private final AppointmentRepository appointmentRepository;
	private final CustomerRepository customerRepository;
	private final AppointmentMapper appointmentMapper;

	public DashboardServiceImpl(AppointmentRepository appointmentRepository, CustomerRepository customerRepository,
			AppointmentMapper appointmentMapper) {
		this.appointmentRepository = appointmentRepository;
		this.customerRepository = customerRepository;
		this.appointmentMapper = appointmentMapper;
	}

	@Override
	@Transactional
	public DashboardResponse getDashboard(Long customerId) {
		appointmentRepository.completeExpiredAppointments(customerId, LocalDate.now(), LocalTime.now(),
				Appointment.Status.CONFIRMED, Appointment.Status.COMPLETED);
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
		List<AppointmentSummary> all = appointmentRepository.findSummariesByCustomerId(customerId);

		List<AppointmentSummary> futureConfirmed = all.stream()
				.filter(appointment -> appointment.status() == Appointment.Status.CONFIRMED)
				.filter(appointment -> appointment.date().isAfter(LocalDate.now())
						|| (appointment.date().isEqual(LocalDate.now())
								&& appointment.startTime().isAfter(LocalTime.now())))
				.sorted(Comparator.comparing(AppointmentSummary::date)
						.thenComparing(AppointmentSummary::startTime))
				.toList();

		List<AppointmentResponse> recent = all.stream()
				.limit(3)
				.map(appointmentMapper::toResponse)
				.toList();
		AppointmentResponse next = futureConfirmed.isEmpty()
				? null
				: appointmentMapper.toResponse(futureConfirmed.get(0));
		long completed = all.stream()
				.filter(appointment -> appointment.status() == Appointment.Status.COMPLETED).count();
		long cancelled = all.stream()
				.filter(appointment -> appointment.status() == Appointment.Status.CANCELLED).count();
		return new DashboardResponse(customer.getFirstName(), futureConfirmed.size(), completed, cancelled,
				next, recent);
	}
}
