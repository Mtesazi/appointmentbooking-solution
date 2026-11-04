package com.appointmentbooking.appointment.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.appointmentbooking.appointment.entity.Customer;

@Service
public class TokenService {
	private static final Duration TOKEN_LIFETIME = Duration.ofHours(1);
	private final JwtEncoder jwtEncoder;

	public TokenService(JwtEncoder jwtEncoder) {
		this.jwtEncoder = jwtEncoder;
	}

	public String createToken(Customer customer) {
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(customer.getId().toString())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(TOKEN_LIFETIME))
				.claim("email", customer.getEmail())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	public long getTokenLifetimeSeconds() {
		return TOKEN_LIFETIME.toSeconds();
	}
}
