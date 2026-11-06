package com.appointmentbooking.appointment.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.AdminAppointmentResponse;
import com.appointmentbooking.appointment.dto.AdminAppointmentSummary;
import com.appointmentbooking.appointment.dto.AdminDashboardResponse;
import com.appointmentbooking.appointment.entity.Appointment;
import com.appointmentbooking.appointment.mapper.AdminAppointmentMapper;
import com.appointmentbooking.appointment.repository.AppointmentRepository;
import com.appointmentbooking.appointment.repository.CustomerRepository;
import com.appointmentbooking.appointment.repository.OfferingRepository;
import com.appointmentbooking.appointment.service.AdminService;

@Service
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {
	private final AppointmentRepository appointmentRepository;
	private final CustomerRepository customerRepository;
	private final OfferingRepository offeringRepository;
	private final AdminAppointmentMapper appointmentMapper;

	public AdminServiceImpl(AppointmentRepository appointmentRepository, CustomerRepository customerRepository,
			OfferingRepository offeringRepository, AdminAppointmentMapper appointmentMapper) {
		this.appointmentRepository = appointmentRepository;
		this.customerRepository = customerRepository;
		this.offeringRepository = offeringRepository;
		this.appointmentMapper = appointmentMapper;
	}

	@Override
	@Transactional
	public AdminDashboardResponse getDashboard() {
		completeExpiredAppointments();
		return new AdminDashboardResponse(
				appointmentRepository.count(),
				appointmentRepository.countUpcoming(LocalDate.now(), LocalTime.now()),
				customerRepository.countCustomers(),
				offeringRepository.countByActiveTrue());
	}

	@Override
	@Transactional
	public List<AdminAppointmentResponse> getAppointments() {
		completeExpiredAppointments();
		return appointmentRepository.findAdminAppointments().stream()
				.map(appointmentMapper::toResponse)
				.toList();
	}

	private void completeExpiredAppointments() {
		appointmentRepository.completeAllExpiredAppointments(LocalDate.now(), LocalTime.now(),
				Appointment.Status.CONFIRMED, Appointment.Status.COMPLETED);
	}
}
