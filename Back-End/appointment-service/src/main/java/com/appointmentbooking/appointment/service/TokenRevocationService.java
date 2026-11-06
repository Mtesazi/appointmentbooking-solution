package com.appointmentbooking.appointment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.appointmentbooking.appointment.entity.RevokedToken;
import com.appointmentbooking.appointment.repository.RevokedTokenRepository;

@Service
public class TokenRevocationService {
	private final RevokedTokenRepository revokedTokenRepository;

	public TokenRevocationService(RevokedTokenRepository revokedTokenRepository) {
		this.revokedTokenRepository = revokedTokenRepository;
	}

	public boolean isRevoked(String token) {
		return revokedTokenRepository.existsByTokenHash(hashToken(token));
	}

	@Transactional
	public void revoke(String token, Instant expiresAt) {
		Instant now = Instant.now();
		revokedTokenRepository.deleteAllByExpiresAtBefore(now);
		revokedTokenRepository.save(new RevokedToken(hashToken(token), expiresAt));
	}

	private String hashToken(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}
}
