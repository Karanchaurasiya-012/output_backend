package com.output_backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CookieBearerTokenResolverTest {

	private final CookieBearerTokenResolver resolver = new CookieBearerTokenResolver();

	@Test
	void prefersHostAccessTokenCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new jakarta.servlet.http.Cookie(CookieBearerTokenResolver.ACCESS_TOKEN_COOKIE, "cookie-jwt"));
		request.addHeader("Authorization", "Bearer header-jwt");
		assertThat(resolver.resolve(request)).isEqualTo("cookie-jwt");
	}

	@Test
	void fallsBackToAuthorizationHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer header-jwt");
		assertThat(resolver.resolve(request)).isEqualTo("header-jwt");
	}

	@Test
	void returnsNullWhenNoTokenPresent() {
		assertThat(resolver.resolve(new MockHttpServletRequest())).isNull();
	}
}
