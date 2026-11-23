package com.output_backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.util.StringUtils;

/**
 * Prefers AuthBackend HttpOnly access-token cookie, then Authorization Bearer header.
 * Cookie name must match AuthBackend {@code __Host-access_token}.
 */
public class CookieBearerTokenResolver implements BearerTokenResolver {

	public static final String ACCESS_TOKEN_COOKIE = "__Host-access_token";

	private final BearerTokenResolver authorizationHeaderResolver = new DefaultBearerTokenResolver();

	@Override
	public String resolve(HttpServletRequest request) {
		String cookieToken = resolveFromCookie(request);
		if (StringUtils.hasText(cookieToken)) {
			return cookieToken;
		}
		return authorizationHeaderResolver.resolve(request);
	}

	private static String resolveFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
