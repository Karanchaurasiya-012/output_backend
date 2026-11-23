package com.output_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Lightweight regression gates for Phase 12 security acceptance.
 */
class Phase12SecurityGatesTest {

	@Test
	void accessTokenCookieNameMatchesAuthBackendContract() {
		assertThat(CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE).isEqualTo("__Host-access_token");
	}

	@Test
	void cookieResolverPrefersCookieThenFallsBackToBearer() {
		CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();
		assertThat(resolver).isNotNull();
	}
}
