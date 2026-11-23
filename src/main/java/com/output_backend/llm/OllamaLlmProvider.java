package com.output_backend.llm;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Ollama {@code /api/chat} client for Qwen 3.5 4B. No silent cloud fallback.
 */
@Component
public class OllamaLlmProvider implements LlmProvider {

	private static final Logger log = LoggerFactory.getLogger(OllamaLlmProvider.class);

	private final RestClient restClient;
	private final String model;

	public OllamaLlmProvider(AppProperties appProperties) {
		AppProperties.Llm.Ollama ollama = appProperties.getLlm().getOllama();
		String baseUrl = ollama.getBaseUrl() == null || ollama.getBaseUrl().isBlank()
				? "http://127.0.0.1:11434"
				: ollama.getBaseUrl().replaceAll("/$", "");
		this.model = ollama.getModel() == null || ollama.getModel().isBlank()
				? "qwen3.5:4b"
				: ollama.getModel().trim();

		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
		requestFactory.setReadTimeout(Duration.ofSeconds(180));
		this.restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.requestFactory(requestFactory)
				.build();
	}

	@Override
	public LlmProviderId id() {
		return LlmProviderId.OLLAMA;
	}

	@Override
	public String model() {
		return model;
	}

	@Override
	public LlmGenerationResult generate(String systemPrompt, String userPrompt) {
		if (userPrompt == null || userPrompt.isBlank()) {
			throw new BusinessException("LLM user prompt is required.", HttpStatus.BAD_REQUEST);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", model);
		body.put("stream", false);
		body.put("messages", List.of(
				Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt),
				Map.of("role", "user", "content", userPrompt)
		));

		try {
			OllamaChatResponse response = restClient.post()
					.uri("/api/chat")
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(OllamaChatResponse.class);

			if (response == null || response.message() == null || response.message().content() == null
					|| response.message().content().isBlank()) {
				throw new BusinessException("Ollama returned an empty chat response.", HttpStatus.BAD_GATEWAY);
			}

			return new LlmGenerationResult(
					response.message().content().strip(),
					id().name().toLowerCase(),
					model
			);
		}
		catch (BusinessException ex) {
			throw ex;
		}
		catch (RestClientResponseException ex) {
			log.warn("Ollama chat HTTP error status={} model={}", ex.getStatusCode().value(), model);
			throw new BusinessException(
					"Ollama chat request failed with HTTP " + ex.getStatusCode().value() + ".",
					HttpStatus.BAD_GATEWAY
			);
		}
		catch (RestClientException ex) {
			log.warn("Ollama chat transport error type={} model={}", ex.getClass().getSimpleName(), model);
			throw new BusinessException(
					"Unable to reach Ollama chat endpoint.",
					HttpStatus.BAD_GATEWAY
			);
		}
	}

	public record OllamaChatResponse(Message message) {
		public record Message(String role, String content) {
		}
	}
}
