package com.output_backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	public static final String BEARER_SCHEME = "bearer-jwt";

	@Bean
	OpenAPI ragOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("RAG Query Service")
						.version("v1")
						.description(
								"Authenticated RAG APIs for chat, conversations, retrieval, and caching. "
										+ "Send AuthBackend JWT via Authorization Bearer or __Host-access_token cookie."
						))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
				.components(new Components().addSecuritySchemes(
						BEARER_SCHEME,
						new SecurityScheme()
								.name(BEARER_SCHEME)
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
				));
	}
}
