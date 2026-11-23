package com.output_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class CurrentUserTest {

	private final CurrentUser currentUser = new CurrentUser();

	@AfterEach
	void clearContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void requireUserId_readsJwtSubject() {
		UUID userId = UUID.randomUUID();
		Jwt jwt = Jwt.withTokenValue("t")
				.header("alg", "RS256")
				.subject(userId.toString())
				.claim("aud", "auth-api")
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		assertThat(currentUser.requireUserId()).isEqualTo(userId);
	}

	@Test
	void requireUserId_rejectsMissingAuth() {
		assertThatThrownBy(currentUser::requireUserId)
				.isInstanceOf(ResponseStatusException.class)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void requireUserId_rejectsNonJwtPrincipal() {
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken("user", "n/a", List.of()));

		assertThatThrownBy(currentUser::requireUserId)
				.isInstanceOf(ResponseStatusException.class);
	}

	@Test
	void requireUserId_rejectsNonUuidSubject() {
		Jwt jwt = Jwt.withTokenValue("t")
				.header("alg", "RS256")
				.subject("not-a-uuid")
				.build();
		SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

		assertThatThrownBy(currentUser::requireUserId)
				.isInstanceOf(ResponseStatusException.class);
	}
}
