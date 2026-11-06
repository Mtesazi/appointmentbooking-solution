package com.appointmentbooking.appointment.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {
	@Test
	void jwtRoleClaimBecomesRoleAuthority() {
		Jwt jwt = Jwt.withTokenValue("token")
				.header("alg", "HS256")
				.claim("role", "ADMIN")
				.build();

		var authentication = new SecurityConfig().jwtAuthenticationConverter().convert(jwt);

		assertThat(authentication).isNotNull();
		assertThat(authentication.getAuthorities())
				.extracting("authority")
				.contains("ROLE_ADMIN");
	}
}
