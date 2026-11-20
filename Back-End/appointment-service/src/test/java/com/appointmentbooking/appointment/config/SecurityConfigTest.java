package com.appointmentbooking.appointment.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigTest {
	@Test
	void corsAllowsLocalDevelopmentAndComposeOrigins() {
		var request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
		var cors = new SecurityConfig().corsConfigurationSource().getCorsConfiguration(request);

		assertThat(cors).isNotNull();
		assertThat(cors.checkOrigin("http://localhost:4200")).isEqualTo("http://localhost:4200");
		assertThat(cors.checkOrigin("http://127.0.0.1:4200")).isEqualTo("http://127.0.0.1:4200");
		assertThat(cors.checkOrigin("http://localhost:4201")).isEqualTo("http://localhost:4201");
		assertThat(cors.checkOrigin("http://127.0.0.1:4201")).isEqualTo("http://127.0.0.1:4201");
		assertThat(cors.checkOrigin("http://example.com")).isNull();
	}

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
