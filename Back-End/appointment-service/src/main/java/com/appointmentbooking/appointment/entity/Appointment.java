package com.appointmentbooking.appointment.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment {
	public enum Status {
		CONFIRMED, COMPLETED, CANCELLED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "customer_id", nullable = false)
	private Long customerId;

	@Column(name = "branch_id", nullable = false)
	private Long branchId;

	@Column(name = "service_id", nullable = false)
	private Long serviceId;

	@Column(name = "appointment_date", nullable = false)
	private LocalDate appointmentDate;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Status status;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected Appointment() {
	}

	public Appointment(Long customerId, Long branchId, Long serviceId, LocalDate appointmentDate,
			LocalTime startTime, LocalTime endTime, String notes) {
		this.customerId = customerId;
		this.branchId = branchId;
		this.serviceId = serviceId;
		this.appointmentDate = appointmentDate;
		this.startTime = startTime;
		this.endTime = endTime;
		this.status = Status.CONFIRMED;
		this.notes = notes;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public Long getBranchId() {
		return branchId;
	}

	public Long getServiceId() {
		return serviceId;
	}

	public LocalDate getAppointmentDate() {
		return appointmentDate;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public Status getStatus() {
		return status;
	}

	public String getNotes() {
		return notes;
	}

	public void cancel() {
		if (status != Status.CONFIRMED) {
			throw new IllegalStateException("Only confirmed appointments can be cancelled");
		}
		status = Status.CANCELLED;
	}

	public void reschedule(LocalDate date, LocalTime start, LocalTime end) {
		if (status != Status.CONFIRMED) {
			throw new IllegalStateException("Only confirmed appointments can be rescheduled");
		}
		appointmentDate = date;
		startTime = start;
		endTime = end;
	}
}
