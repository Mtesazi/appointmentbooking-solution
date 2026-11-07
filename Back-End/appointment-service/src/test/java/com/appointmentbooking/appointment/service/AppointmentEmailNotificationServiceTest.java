package com.appointmentbooking.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import com.appointmentbooking.appointment.entity.Appointment;
import com.appointmentbooking.appointment.entity.Branch;
import com.appointmentbooking.appointment.entity.Customer;
import com.appointmentbooking.appointment.entity.EmailOutboxMessage;
import com.appointmentbooking.appointment.entity.Offering;
import com.appointmentbooking.appointment.repository.BranchRepository;
import com.appointmentbooking.appointment.repository.CustomerRepository;
import com.appointmentbooking.appointment.repository.EmailOutboxRepository;
import com.appointmentbooking.appointment.repository.OfferingRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AppointmentEmailNotificationServiceTest {
	private final CustomerRepository customers = org.mockito.Mockito.mock(CustomerRepository.class);
	private final BranchRepository branches = org.mockito.Mockito.mock(BranchRepository.class);
	private final OfferingRepository offerings = org.mockito.Mockito.mock(OfferingRepository.class);
	private final EmailOutboxRepository outbox = org.mockito.Mockito.mock(EmailOutboxRepository.class);

	@Test
	void queuesAppointmentConfirmationWithAppointmentDetails() {
		var service = new AppointmentEmailNotificationService(true, customers, branches, offerings, outbox);
		var appointment = org.mockito.Mockito.mock(Appointment.class);
		when(appointment.getCustomerId()).thenReturn(7L);
		when(appointment.getBranchId()).thenReturn(8L);
		when(appointment.getServiceId()).thenReturn(9L);
		when(appointment.getId()).thenReturn(10L);
		when(appointment.getAppointmentDate()).thenReturn(LocalDate.of(2026, 12, 3));
		when(appointment.getStartTime()).thenReturn(LocalTime.of(14, 30));
		when(customers.findById(7L)).thenReturn(Optional.of(new Customer("Alex", "Customer",
				"alex@example.com", "1234567", "hash")));
		when(branches.findById(8L)).thenReturn(Optional.of(new Branch("Central", true)));
		when(offerings.findById(9L)).thenReturn(Optional.of(new Offering("Consultation", 30, true)));

		service.queue(appointment, AppointmentEmailNotificationService.Event.BOOKED);

		var message = ArgumentCaptor.forClass(EmailOutboxMessage.class);
		verify(outbox).save(message.capture());
		assertEquals("alex@example.com", message.getValue().getRecipient());
		assertEquals("Appointment booking confirmed", message.getValue().getSubject());
		org.junit.jupiter.api.Assertions.assertTrue(message.getValue().getBody().contains("Appointment: #10"));
		org.junit.jupiter.api.Assertions.assertTrue(message.getValue().getBody().contains("Date: December 3, 2026"));
		org.junit.jupiter.api.Assertions.assertTrue(message.getValue().getBody().contains("Time: 2:30 PM"));
	}

	@Test
	void doesNotLookUpOrQueueAnythingWhenEmailIsDisabled() {
		var service = new AppointmentEmailNotificationService(false, customers, branches, offerings, outbox);

		service.queue(org.mockito.Mockito.mock(Appointment.class),
				AppointmentEmailNotificationService.Event.CANCELLED);

		verifyNoInteractions(customers, branches, offerings, outbox);
	}

	@Test
	void queuesCancellationNotice() {
		var service = new AppointmentEmailNotificationService(true, customers, branches, offerings, outbox);
		var appointment = org.mockito.Mockito.mock(Appointment.class);
		when(appointment.getCustomerId()).thenReturn(7L);
		when(appointment.getBranchId()).thenReturn(8L);
		when(appointment.getServiceId()).thenReturn(9L);
		when(appointment.getId()).thenReturn(10L);
		when(appointment.getAppointmentDate()).thenReturn(LocalDate.of(2026, 12, 3));
		when(appointment.getStartTime()).thenReturn(LocalTime.of(14, 30));
		when(customers.findById(7L)).thenReturn(Optional.of(new Customer("Alex", "Customer",
				"alex@example.com", "1234567", "hash")));
		when(branches.findById(8L)).thenReturn(Optional.of(new Branch("Central", true)));
		when(offerings.findById(9L)).thenReturn(Optional.of(new Offering("Consultation", 30, true)));

		service.queue(appointment, AppointmentEmailNotificationService.Event.CANCELLED);

		var message = ArgumentCaptor.forClass(EmailOutboxMessage.class);
		verify(outbox).save(message.capture());
		assertEquals("Appointment cancelled", message.getValue().getSubject());
	}
}
