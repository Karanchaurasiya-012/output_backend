package com.output_backend.common.web;

import java.util.Map;

import com.output_backend.common.response.ApiResponse;
import com.output_backend.config.AppProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

	private final AppProperties appProperties;

	public HealthController(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@GetMapping("/health")
	public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
		Map<String, Object> data = Map.of(
				"service", "output_backend",
				"status", "UP",
				"defaultEmbeddingProvider", appProperties.getEmbedding().getDefaultProvider(),
				"defaultLlmProvider", appProperties.getLlm().getDefaultProvider(),
				"embeddingDimensions", appProperties.getEmbedding().getDimensions()
		);
		return ResponseEntity.ok(ApiResponse.ok("RAG query service is up.", data));
	}
}
