package com.appointmentbooking.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "branches")
public class Branch {
	@Id
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String name;

	@Column(nullable = false)
	private boolean active;

	protected Branch() {
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isActive() {
		return active;
	}
}
