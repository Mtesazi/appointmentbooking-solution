package com.appointmentbooking.appointment.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_outbox")
public class EmailOutboxMessage {
	public enum Status {
		PENDING, FAILED, SENT, DEAD
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 254)
	private String recipient;

	@Column(nullable = false, length = 200)
	private String subject;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status = Status.PENDING;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "next_attempt_at", nullable = false)
	private Instant nextAttemptAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "sent_at")
	private Instant sentAt;

	@Column(name = "last_error", length = 120)
	private String lastError;

	protected EmailOutboxMessage() {
	}

	public EmailOutboxMessage(String recipient, String subject, String body) {
		this.recipient = recipient;
		this.subject = subject;
		this.body = body;
		this.status = Status.PENDING;
		this.attemptCount = 0;
		this.nextAttemptAt = Instant.now();
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getRecipient() {
		return recipient;
	}

	public String getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public Status getStatus() {
		return status;
	}

	public int getAttemptCount() {
		return attemptCount;
	}

	public void markSent(Instant sentAt) {
		this.status = Status.SENT;
		this.sentAt = sentAt;
		this.lastError = null;
	}

	public void markFailed(Instant nextAttemptAt, String errorType, int maxAttempts) {
		attemptCount++;
		this.lastError = errorType;
		this.status = attemptCount >= maxAttempts ? Status.DEAD : Status.FAILED;
		this.nextAttemptAt = nextAttemptAt;
	}
}
