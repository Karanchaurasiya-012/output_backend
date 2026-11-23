package com.output_backend.conversation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.common.exception.GlobalExceptionHandler;
import com.output_backend.config.AppProperties;
import com.output_backend.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ConversationController.class)
@Import({
		CurrentUser.class,
		GlobalExceptionHandler.class,
		ConversationOwnershipWebMvcTest.TestSecurityConfig.class
})
class ConversationOwnershipWebMvcTest {

	private static final UUID OWNER_A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID OWNER_B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID CONVERSATION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ConversationService conversationService;

	@Test
	void listRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/conversations"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void listReturnsOnlyCallerOwnedConversationsViaService() throws Exception {
		Conversation conversation = new Conversation();
		conversation.setId(CONVERSATION_ID);
		conversation.setOwnerUserId(OWNER_A);
		conversation.setTitle("Mine");
		when(conversationService.list(eq(OWNER_A), eq(0), eq(20)))
				.thenReturn(new PageImpl<>(List.of(conversation), PageRequest.of(0, 20), 1));
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("conversationId", CONVERSATION_ID.toString());
		summary.put("title", "Mine");
		when(conversationService.toSummary(conversation)).thenReturn(summary);

		mockMvc.perform(get("/api/v1/conversations")
						.with(jwt().jwt(j -> j.subject(OWNER_A.toString()))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.items[0].conversationId").value(CONVERSATION_ID.toString()));

		verify(conversationService).list(OWNER_A, 0, 20);
	}

	@Test
	void messagesRejectCrossUserAccess() throws Exception {
		when(conversationService.listMessages(CONVERSATION_ID, OWNER_B))
				.thenThrow(new BusinessException("Conversation not found.", HttpStatus.NOT_FOUND));

		mockMvc.perform(get("/api/v1/conversations/{id}/messages", CONVERSATION_ID)
						.with(jwt().jwt(j -> j.subject(OWNER_B.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.message").value("Conversation not found."));
	}

	@Test
	void deleteRejectsCrossUserAccess() throws Exception {
		doThrow(new BusinessException("Conversation not found.", HttpStatus.NOT_FOUND))
				.when(conversationService).delete(CONVERSATION_ID, OWNER_B);

		mockMvc.perform(delete("/api/v1/conversations/{id}", CONVERSATION_ID)
						.with(jwt().jwt(j -> j.subject(OWNER_B.toString()))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false));
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
					.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
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
				throw new BadJwtException("stub decoder — use jwt() post-processor");
			};
		}
	}
}
