package com.output_backend.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.output_backend.common.web.HealthController;
import com.output_backend.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {MeController.class, HealthController.class})
@Import({
		CurrentUser.class,
		JwtSecurityWebMvcTest.TestSecurityConfig.class
})
class JwtSecurityWebMvcTest {

	private static final UUID USER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void healthIsPublic() throws Exception {
		mockMvc.perform(get("/api/v1/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));
	}

	@Test
	void missingJwtOnMeReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void invalidBearerTokenReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/me")
						.header("Authorization", "Bearer not-a-real-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedMeReturnsJwtSubject() throws Exception {
		mockMvc.perform(get("/api/v1/me")
						.with(jwt().jwt(j -> j.subject(USER_A.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.userId").value(USER_A.toString()));
	}

	static class TestSecurityConfig {
		@Bean
		AppProperties appProperties() {
			return new AppProperties();
		}

		@Bean
		SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
			BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
			http
					.csrf(csrf -> csrf.disable())
					.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
					.authorizeHttpRequests(auth -> auth
							.requestMatchers("/api/v1/health").permitAll()
							.anyRequest().authenticated()
					)
					.oauth2ResourceServer(oauth2 -> oauth2
							.jwt(Customizer.withDefaults())
							.authenticationEntryPoint(entryPoint)
					)
					.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint));
			return http.build();
		}

		@Bean
		JwtDecoder jwtDecoder() {
			return token -> {
				throw new BadJwtException("invalid token");
			};
		}
	}
}
