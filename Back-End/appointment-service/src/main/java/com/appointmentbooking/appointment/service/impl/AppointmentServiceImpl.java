package com.appointmentbooking.appointment.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.CreateAppointmentRequest;
import com.appointmentbooking.appointment.entity.Appointment;
import com.appointmentbooking.appointment.entity.Branch;
import com.appointmentbooking.appointment.entity.Offering;
import com.appointmentbooking.appointment.exception.BadRequestException;
import com.appointmentbooking.appointment.exception.ConflictException;
import com.appointmentbooking.appointment.exception.ResourceNotFoundException;
import com.appointmentbooking.appointment.mapper.AppointmentMapper;
import com.appointmentbooking.appointment.repository.AppointmentRepository;
import com.appointmentbooking.appointment.repository.BranchRepository;
import com.appointmentbooking.appointment.repository.OfferingRepository;
import com.appointmentbooking.appointment.service.AppointmentService;

@Service
public class AppointmentServiceImpl implements AppointmentService {
	private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a");
	private static final LocalTime OPENING_TIME = LocalTime.of(9, 0);
	private static final LocalTime CLOSING_TIME = LocalTime.of(17, 0);
	private final AppointmentRepository appointmentRepository;
	private final BranchRepository branchRepository;
	private final OfferingRepository offeringRepository;
	private final AppointmentMapper appointmentMapper;

	public AppointmentServiceImpl(AppointmentRepository appointmentRepository, BranchRepository branchRepository,
			OfferingRepository offeringRepository, AppointmentMapper appointmentMapper) {
		this.appointmentRepository = appointmentRepository;
		this.branchRepository = branchRepository;
		this.offeringRepository = offeringRepository;
		this.appointmentMapper = appointmentMapper;
	}

	@Override
	@Transactional
	public AppointmentResponse create(Long customerId, CreateAppointmentRequest request) {
		Branch branch = branchRepository.findById(request.branchId())
				.filter(Branch::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		Offering offering = offeringRepository.findById(request.serviceId())
				.filter(Offering::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));

		LocalTime startTime = parseStartTime(request.time());
		LocalTime endTime = startTime.plusMinutes(offering.getDurationMinutes());
		validateBookingTime(request.appointmentDate(), startTime, endTime);
		if (appointmentRepository.existsOverlappingAppointment(branch.getId(), request.appointmentDate(),
				startTime, endTime, Appointment.Status.CONFIRMED)) {
			throw new ConflictException("This time is no longer available at the selected branch");
		}

		Appointment appointment = appointmentRepository.save(new Appointment(customerId, branch.getId(),
				offering.getId(), request.appointmentDate(), startTime, endTime, request.notes()));
		return appointmentMapper.toResponse(appointment, branch, offering);
	}

	@Override
	@Transactional
	public List<AppointmentResponse> getForCustomer(Long customerId) {
		appointmentRepository.completeExpiredAppointments(customerId, LocalDate.now(), LocalTime.now(),
				Appointment.Status.CONFIRMED, Appointment.Status.COMPLETED);
		return appointmentRepository.findAllByCustomerIdOrderByAppointmentDateDescStartTimeDesc(customerId).stream()
				.map(this::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public AppointmentResponse cancel(Long customerId, Long appointmentId) {
		appointmentRepository.completeExpiredAppointments(customerId, LocalDate.now(), LocalTime.now(),
				Appointment.Status.CONFIRMED, Appointment.Status.COMPLETED);
		Appointment appointment = appointmentRepository.findByIdAndCustomerId(appointmentId, customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
		try {
			appointment.cancel();
		} catch (IllegalStateException exception) {
			throw new ConflictException(exception.getMessage());
		}
		return toResponse(appointment);
	}

	private AppointmentResponse toResponse(Appointment appointment) {
		Branch branch = branchRepository.findById(appointment.getBranchId())
				.orElseThrow(() -> new ResourceNotFoundException("Appointment branch not found"));
		Offering offering = offeringRepository.findById(appointment.getServiceId())
				.orElseThrow(() -> new ResourceNotFoundException("Appointment service not found"));
		return appointmentMapper.toResponse(appointment, branch, offering);
	}

	private LocalTime parseStartTime(String value) {
		try {
			return LocalTime.parse(value.trim(), DISPLAY_TIME);
		} catch (DateTimeParseException displayFormatException) {
			try {
				return LocalTime.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_TIME);
			} catch (DateTimeParseException isoFormatException) {
				throw new BadRequestException("time must use h:mm AM/PM or HH:mm format");
			}
		}
	}

	private void validateBookingTime(LocalDate date, LocalTime startTime, LocalTime endTime) {
		if (startTime.isBefore(OPENING_TIME) || endTime.isAfter(CLOSING_TIME)) {
			throw new BadRequestException("Appointment time must be within branch opening hours (9:00 AM to 5:00 PM)");
		}
		if (!date.isAfter(LocalDate.now()) && !LocalDateTime.of(date, startTime).isAfter(LocalDateTime.now())) {
			throw new BadRequestException("Appointment time must be in the future");
		}
	}
}
