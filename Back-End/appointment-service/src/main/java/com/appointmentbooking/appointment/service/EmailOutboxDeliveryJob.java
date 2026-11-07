package com.appointmentbooking.appointment.service;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.entity.EmailOutboxMessage;
import com.appointmentbooking.appointment.repository.EmailOutboxRepository;

@Component
@ConditionalOnProperty(name = "app.notifications.email.enabled", havingValue = "true")
public class EmailOutboxDeliveryJob {
	private static final Logger logger = LoggerFactory.getLogger(EmailOutboxDeliveryJob.class);
	private static final int MAX_ATTEMPTS = 10;

	private final EmailOutboxRepository repository;
	private final JavaMailSender mailSender;
	private final String fromAddress;

	public EmailOutboxDeliveryJob(EmailOutboxRepository repository, JavaMailSender mailSender,
			@Value("${app.notifications.email.from}") String fromAddress,
			@Value("${spring.mail.host:}") String mailHost) {
		if (fromAddress.isBlank()) {
			throw new IllegalStateException("EMAIL_FROM must be configured when email notifications are enabled");
		}
		if (mailHost.isBlank()) {
			throw new IllegalStateException("MAIL_HOST must be configured when email notifications are enabled");
		}
		this.repository = repository;
		this.mailSender = mailSender;
		this.fromAddress = fromAddress;
	}

	@Scheduled(fixedDelayString = "${app.notifications.email.poll-interval-ms:5000}")
	@Transactional
	public void deliverPendingEmails() {
		for (EmailOutboxMessage message : repository.findNextBatchForDelivery()) {
			SimpleMailMessage mail = new SimpleMailMessage();
			mail.setFrom(fromAddress);
			mail.setTo(message.getRecipient());
			mail.setSubject(message.getSubject());
			mail.setText(message.getBody());
			try {
				mailSender.send(mail);
				message.markSent(Instant.now());
			} catch (MailException exception) {
				int failedAttempt = message.getAttemptCount() + 1;
				long delaySeconds = Math.min(21_600L, 30L * (1L << Math.min(failedAttempt, 10)));
				message.markFailed(Instant.now().plusSeconds(delaySeconds),
						exception.getClass().getSimpleName(), MAX_ATTEMPTS);
				logger.warn("Email outbox message {} failed on attempt {} ({})",
						message.getId(), failedAttempt, exception.getClass().getSimpleName());
			}
		}
	}
}
