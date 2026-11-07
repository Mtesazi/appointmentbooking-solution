package com.appointmentbooking.appointment.service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.appointmentbooking.appointment.entity.Appointment;
import com.appointmentbooking.appointment.entity.Branch;
import com.appointmentbooking.appointment.entity.Customer;
import com.appointmentbooking.appointment.entity.EmailOutboxMessage;
import com.appointmentbooking.appointment.entity.Offering;
import com.appointmentbooking.appointment.repository.BranchRepository;
import com.appointmentbooking.appointment.repository.CustomerRepository;
import com.appointmentbooking.appointment.repository.EmailOutboxRepository;
import com.appointmentbooking.appointment.repository.OfferingRepository;

@Service
public class AppointmentEmailNotificationService {
	public enum Event {
		BOOKED("Appointment booking confirmed"),
		RESCHEDULED("Appointment rescheduled"),
		CANCELLED("Appointment cancelled");

		private final String subject;

		Event(String subject) {
			this.subject = subject;
		}
	}

	private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);
	private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);

	private final boolean enabled;
	private final CustomerRepository customerRepository;
	private final BranchRepository branchRepository;
	private final OfferingRepository offeringRepository;
	private final EmailOutboxRepository emailOutboxRepository;

	public AppointmentEmailNotificationService(
			@Value("${app.notifications.email.enabled:false}") boolean enabled,
			CustomerRepository customerRepository,
			BranchRepository branchRepository,
			OfferingRepository offeringRepository,
			EmailOutboxRepository emailOutboxRepository) {
		this.enabled = enabled;
		this.customerRepository = customerRepository;
		this.branchRepository = branchRepository;
		this.offeringRepository = offeringRepository;
		this.emailOutboxRepository = emailOutboxRepository;
	}

	public void queue(Appointment appointment, Event event) {
		if (!enabled) {
			return;
		}
		Customer customer = customerRepository.findById(appointment.getCustomerId())
				.orElseThrow(() -> new IllegalStateException("Appointment customer not found"));
		Branch branch = branchRepository.findById(appointment.getBranchId())
				.orElseThrow(() -> new IllegalStateException("Appointment branch not found"));
		Offering offering = offeringRepository.findById(appointment.getServiceId())
				.orElseThrow(() -> new IllegalStateException("Appointment service not found"));

		String body = """
				Hello %s,

				%s

				Appointment: #%d
				Service: %s
				Branch: %s
				Date: %s
				Time: %s

				Thank you,
				Appointment Booking
				""".formatted(customer.getFirstName(), event.subject, appointment.getId(), offering.getName(),
				branch.getName(), appointment.getAppointmentDate().format(DISPLAY_DATE),
				appointment.getStartTime().format(DISPLAY_TIME));
		emailOutboxRepository.save(new EmailOutboxMessage(customer.getEmail(), event.subject, body));
	}
}
