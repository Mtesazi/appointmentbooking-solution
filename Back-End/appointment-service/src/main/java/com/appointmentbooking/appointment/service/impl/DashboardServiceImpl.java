package com.appointmentbooking.appointment.service.impl;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.DashboardResponse;
import com.appointmentbooking.appointment.entity.Appointment;
import com.appointmentbooking.appointment.entity.Branch;
import com.appointmentbooking.appointment.entity.Customer;
import com.appointmentbooking.appointment.entity.Offering;
import com.appointmentbooking.appointment.exception.ResourceNotFoundException;
import com.appointmentbooking.appointment.mapper.AppointmentMapper;
import com.appointmentbooking.appointment.repository.AppointmentRepository;
import com.appointmentbooking.appointment.repository.BranchRepository;
import com.appointmentbooking.appointment.repository.CustomerRepository;
import com.appointmentbooking.appointment.repository.OfferingRepository;
import com.appointmentbooking.appointment.service.DashboardService;

@Service
public class DashboardServiceImpl implements DashboardService {
	private final AppointmentRepository appointmentRepository;
	private final CustomerRepository customerRepository;
	private final BranchRepository branchRepository;
	private final OfferingRepository offeringRepository;
	private final AppointmentMapper appointmentMapper;

	public DashboardServiceImpl(AppointmentRepository appointmentRepository, CustomerRepository customerRepository,
			BranchRepository branchRepository, OfferingRepository offeringRepository,
			AppointmentMapper appointmentMapper) {
		this.appointmentRepository = appointmentRepository;
		this.customerRepository = customerRepository;
		this.branchRepository = branchRepository;
		this.offeringRepository = offeringRepository;
		this.appointmentMapper = appointmentMapper;
	}

	@Override
	@Transactional
	public DashboardResponse getDashboard(Long customerId) {
		appointmentRepository.completeExpiredAppointments(customerId, LocalDate.now(), LocalTime.now(),
				Appointment.Status.CONFIRMED, Appointment.Status.COMPLETED);
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
		List<Appointment> all = appointmentRepository
				.findAllByCustomerIdOrderByAppointmentDateDescStartTimeDesc(customerId);

		List<Appointment> futureConfirmed = all.stream()
				.filter(appointment -> appointment.getStatus() == Appointment.Status.CONFIRMED)
				.filter(appointment -> appointment.getAppointmentDate().isAfter(LocalDate.now())
						|| (appointment.getAppointmentDate().isEqual(LocalDate.now())
								&& appointment.getStartTime().isAfter(LocalTime.now())))
				.sorted(Comparator.comparing(Appointment::getAppointmentDate)
						.thenComparing(Appointment::getStartTime))
				.toList();

		List<AppointmentResponse> recent = all.stream()
				.limit(3)
				.map(this::toResponse)
				.toList();
		AppointmentResponse next = futureConfirmed.isEmpty() ? null : toResponse(futureConfirmed.get(0));
		long completed = all.stream()
				.filter(appointment -> appointment.getStatus() == Appointment.Status.COMPLETED).count();
		long cancelled = all.stream()
				.filter(appointment -> appointment.getStatus() == Appointment.Status.CANCELLED).count();
		return new DashboardResponse(customer.getFirstName(), futureConfirmed.size(), completed, cancelled,
				next, recent);
	}

	private AppointmentResponse toResponse(Appointment appointment) {
		Branch branch = branchRepository.findById(appointment.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Appointment branch not found"));
		Offering offering = offeringRepository.findById(appointment.getServiceId())
				.orElseThrow(() -> new ResourceNotFoundException("Appointment service not found"));
		return appointmentMapper.toResponse(appointment, branch, offering);
	}
}
