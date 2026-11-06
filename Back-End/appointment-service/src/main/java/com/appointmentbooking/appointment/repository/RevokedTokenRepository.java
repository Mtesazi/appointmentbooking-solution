package com.appointmentbooking.appointment.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.appointmentbooking.appointment.entity.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
	boolean existsByTokenHash(String tokenHash);

	void deleteAllByExpiresAtBefore(Instant instant);
}
