package com.appointmentbooking.appointment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;

@Entity
@Table(name = "services")
public class Offering {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 120)
	private String name;

	@Column(name = "duration_minutes", nullable = false)
	private int durationMinutes;

	@Column(nullable = false)
	private boolean active;

	protected Offering() {
	}

	public Offering(String name, int durationMinutes, boolean active) {
		update(name, durationMinutes, active);
	}

	public void update(String name, int durationMinutes, boolean active) {
		this.name = name;
		this.durationMinutes = durationMinutes;
		this.active = active;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public int getDurationMinutes() {
		return durationMinutes;
	}

	public boolean isActive() {
		return active;
	}
}
