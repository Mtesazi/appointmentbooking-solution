package com.appointmentbooking.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.appointmentbooking.appointment.entity.EmailOutboxMessage;
import com.appointmentbooking.appointment.repository.EmailOutboxRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailOutboxDeliveryJobTest {
	private final EmailOutboxRepository repository = org.mockito.Mockito.mock(EmailOutboxRepository.class);
	private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
	private final EmailOutboxDeliveryJob job = new EmailOutboxDeliveryJob(
			repository, mailSender, "noreply@example.com", "smtp.example.com");

	@Test
	void deliversPendingMessageAndMarksItSent() {
		var message = new EmailOutboxMessage("alex@example.com", "Appointment confirmed", "Details");
		when(repository.findNextBatchForDelivery()).thenReturn(List.of(message));

		job.deliverPendingEmails();

		var sentMessage = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(mailSender).send(sentMessage.capture());
		assertEquals("noreply@example.com", sentMessage.getValue().getFrom());
		assertEquals("alex@example.com", sentMessage.getValue().getTo()[0]);
		assertEquals(EmailOutboxMessage.Status.SENT, message.getStatus());
	}

	@Test
	void retriesTemporaryMailFailure() {
		var message = new EmailOutboxMessage("alex@example.com", "Appointment confirmed", "Details");
		when(repository.findNextBatchForDelivery()).thenReturn(List.of(message));
		doThrow(new MailSendException("provider unavailable")).when(mailSender).send(any(SimpleMailMessage.class));

		job.deliverPendingEmails();

		assertEquals(EmailOutboxMessage.Status.FAILED, message.getStatus());
		assertEquals(1, message.getAttemptCount());
	}
}
