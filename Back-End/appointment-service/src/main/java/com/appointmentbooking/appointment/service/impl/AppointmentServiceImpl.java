package com.appointmentbooking.appointment.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.dto.AppointmentResponse;
import com.appointmentbooking.appointment.dto.CreateAppointmentRequest;
import com.appointmentbooking.appointment.dto.RescheduleAppointmentRequest;
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
import com.appointmentbooking.appointment.service.BranchHoursService;
import com.appointmentbooking.appointment.service.BranchHoursService.Hours;

@Service
public class AppointmentServiceImpl implements AppointmentService {
	private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
	private final AppointmentRepository appointmentRepository;
	private final BranchRepository branchRepository;
	private final OfferingRepository offeringRepository;
	private final AppointmentMapper appointmentMapper;
	private final BranchHoursService branchHours;

	public AppointmentServiceImpl(AppointmentRepository appointmentRepository, BranchRepository branchRepository,
			OfferingRepository offeringRepository, AppointmentMapper appointmentMapper, BranchHoursService branchHours) {
		this.appointmentRepository = appointmentRepository;
		this.branchRepository = branchRepository;
		this.offeringRepository = offeringRepository;
		this.appointmentMapper = appointmentMapper;
		this.branchHours = branchHours;
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getAvailableTimes(Long branchId, Long serviceId, LocalDate date) {
		branchRepository.findById(branchId).filter(Branch::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		Offering offering = offeringRepository.findById(serviceId).filter(Offering::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		return availableTimes(branchId, offering, date, null);
	}

	@Override
	@Transactional(readOnly = true)
	public List<String> getRescheduleAvailableTimes(Long customerId, Long appointmentId, LocalDate date) {
		Appointment appointment = appointmentRepository.findByIdAndCustomerId(appointmentId, customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
		validateReschedulable(appointment);
		branchRepository.findById(appointment.getBranchId()).filter(Branch::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		Offering offering = offeringRepository.findById(appointment.getServiceId())
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		return availableTimes(appointment.getBranchId(), offering, date, appointmentId);
	}

	private List<String> availableTimes(Long branchId, Offering offering, LocalDate date, Long excludedId) {
		Hours hours = branchHours.hoursFor(branchId, date);
		if (hours.closed()) {
			return List.of();
		}
		List<Appointment> booked = appointmentRepository.findAllByBranchIdAndAppointmentDateAndStatus(
				branchId, date, Appointment.Status.CONFIRMED);
		LocalDateTime now = LocalDateTime.now();
		return java.util.stream.IntStream.iterate(0, minute -> minute < 24 * 60, minute -> minute + 30)
				.mapToObj(minute -> LocalDateTime.of(date, hours.openTime()).plusMinutes(minute))
				.takeWhile(start -> start.toLocalDate().equals(date)
						&& !start.toLocalTime().isAfter(hours.closeTime()))
				.map(LocalDateTime::toLocalTime)
				.filter(start -> LocalDateTime.of(date, start).isAfter(now))
				.filter(start -> !LocalDateTime.of(date, start).plusMinutes(offering.getDurationMinutes())
						.isAfter(LocalDateTime.of(date, hours.closeTime())))
				.filter(start -> booked.stream().filter(appointment -> !appointment.getId().equals(excludedId))
						.noneMatch(appointment ->
						start.isBefore(appointment.getEndTime())
								&& start.plusMinutes(offering.getDurationMinutes()).isAfter(appointment.getStartTime())))
				.map(DISPLAY_TIME::format)
				.toList();
	}

	@Override
	@Transactional
	public AppointmentResponse reschedule(Long customerId, Long appointmentId, RescheduleAppointmentRequest request) {
		Appointment appointment = appointmentRepository.findByIdAndCustomerIdForUpdate(appointmentId, customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
		validateReschedulable(appointment);
		Branch branch = branchRepository.findByIdForBooking(appointment.getBranchId()).filter(Branch::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		Offering offering = offeringRepository.findByIdForBooking(appointment.getServiceId())
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));
		LocalTime start = parseStartTime(request.time());
		LocalTime end = start.plusMinutes(offering.getDurationMinutes());
		validateBookingTime(branch.getId(), request.appointmentDate(), start, offering.getDurationMinutes());
		if (appointmentRepository.existsOverlappingOtherAppointment(branch.getId(), request.appointmentDate(),
				start, end, Appointment.Status.CONFIRMED, appointmentId)) {
			throw new ConflictException("This time is no longer available at the selected branch");
		}
		appointment.reschedule(request.appointmentDate(), start, end);
		appointmentRepository.flush();
		return appointmentMapper.toResponse(appointment, branch, offering);
	}

	private void validateReschedulable(Appointment appointment) {
		if (appointment.getStatus() != Appointment.Status.CONFIRMED
				|| !LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime())
						.isAfter(LocalDateTime.now())) {
			throw new ConflictException("Only confirmed appointments that have not started can be rescheduled");
		}
	}

	@Override
	@Transactional
	public AppointmentResponse create(Long customerId, CreateAppointmentRequest request) {
		Branch branch = branchRepository.findByIdForBooking(request.branchId())
				.filter(Branch::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
		Offering offering = offeringRepository.findByIdForBooking(request.serviceId())
				.filter(Offering::isActive)
				.orElseThrow(() -> new ResourceNotFoundException("Service not found"));

		LocalTime startTime = parseStartTime(request.time());
		LocalTime endTime = startTime.plusMinutes(offering.getDurationMinutes());
		validateBookingTime(branch.getId(), request.appointmentDate(), startTime, offering.getDurationMinutes());
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
		return appointmentRepository.findSummariesByCustomerId(customerId).stream()
				.map(appointmentMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public AppointmentResponse cancel(Long customerId, Long appointmentId) {
		Appointment appointment = appointmentRepository.findByIdAndCustomerIdForUpdate(appointmentId, customerId)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
		cancelAppointment(appointment);
		return toResponse(appointment);
	}

	@Override
	@Transactional
	public void cancelAsAdmin(Long appointmentId) {
		Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));
		cancelAppointment(appointment);
	}

	private void cancelAppointment(Appointment appointment) {
		if (appointment.getStatus() != Appointment.Status.CONFIRMED
				|| !LocalDateTime.of(appointment.getAppointmentDate(), appointment.getStartTime())
						.isAfter(LocalDateTime.now())) {
			throw new ConflictException("Only confirmed appointments that have not started can be cancelled");
		}
		try {
			appointment.cancel();
		} catch (IllegalStateException exception) {
			throw new ConflictException(exception.getMessage());
		}
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

	private void validateBookingTime(Long branchId, LocalDate date, LocalTime startTime, int durationMinutes) {
		Hours hours = branchHours.hoursFor(branchId, date);
		if (hours.closed() || startTime.isBefore(hours.openTime())
				|| LocalDateTime.of(date, startTime).plusMinutes(durationMinutes)
						.isAfter(LocalDateTime.of(date, hours.closeTime()))) {
			throw new BadRequestException("Appointment time must be within branch opening hours");
		}
		if (!date.isAfter(LocalDate.now()) && !LocalDateTime.of(date, startTime).isAfter(LocalDateTime.now())) {
			throw new BadRequestException("Appointment time must be in the future");
		}
	}
}
