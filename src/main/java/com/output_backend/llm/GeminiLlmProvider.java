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
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google Generative Language {@code generateContent} client for {@code gemini-3.5-flash-lite}.
 * No silent fallback to Ollama.
 */
@Component
public class GeminiLlmProvider implements LlmProvider {

	private static final Logger log = LoggerFactory.getLogger(GeminiLlmProvider.class);
	private static final String BASE_URL = "https://generativelanguage.googleapis.com";

	private final RestClient restClient;
	private final String model;
	private final String apiKey;

	public GeminiLlmProvider(AppProperties appProperties) {
		AppProperties.Llm.Gemini gemini = appProperties.getLlm().getGemini();
		this.model = gemini.getModel() == null || gemini.getModel().isBlank()
				? "gemini-3.5-flash-lite"
				: gemini.getModel().trim();
		this.apiKey = gemini.getApiKey() == null ? "" : gemini.getApiKey().trim();

		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
		requestFactory.setReadTimeout(Duration.ofSeconds(120));
		this.restClient = RestClient.builder()
				.baseUrl(BASE_URL)
				.requestFactory(requestFactory)
				.build();
	}

	@Override
	public LlmProviderId id() {
		return LlmProviderId.GEMINI;
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
		if (apiKey.isBlank()) {
			throw new BusinessException(
					"GEMINI_API_KEY is not configured for the Gemini LLM provider.",
					HttpStatus.SERVICE_UNAVAILABLE
			);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		if (systemPrompt != null && !systemPrompt.isBlank()) {
			body.put("system_instruction", Map.of(
					"parts", List.of(Map.of("text", systemPrompt))
			));
		}
		body.put("contents", List.of(Map.of(
				"role", "user",
				"parts", List.of(Map.of("text", userPrompt))
		)));
		body.put("generationConfig", Map.of(
				"temperature", 0.2,
				"maxOutputTokens", 1024
		));

		String uri = UriComponentsBuilder
				.fromPath("/v1beta/models/{model}:generateContent")
				.queryParam("key", apiKey)
				.buildAndExpand(model)
				.toUriString();

		try {
			GeminiGenerateResponse response = restClient.post()
					.uri(uri)
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(GeminiGenerateResponse.class);

			String text = extractText(response);
			if (text == null || text.isBlank()) {
				throw new BusinessException("Gemini returned an empty chat response.", HttpStatus.BAD_GATEWAY);
			}
			return new LlmGenerationResult(text.strip(), id().name().toLowerCase(), model);
		}
		catch (BusinessException ex) {
			throw ex;
		}
		catch (RestClientResponseException ex) {
			int status = ex.getStatusCode().value();
			log.warn("Gemini chat HTTP error status={} model={}", status, model);
			if (status == 429) {
				throw new BusinessException(
						"Gemini rate limit exceeded. Retry later.",
						HttpStatus.TOO_MANY_REQUESTS
				);
			}
			if (status == 401 || status == 403) {
				throw new BusinessException(
						"Gemini authentication failed for chat generation.",
						HttpStatus.BAD_GATEWAY
				);
			}
			throw new BusinessException(
					"Gemini chat request failed with HTTP " + status + ".",
					HttpStatus.BAD_GATEWAY
			);
		}
		catch (RestClientException ex) {
			log.warn("Gemini chat transport error type={} model={}", ex.getClass().getSimpleName(), model);
			throw new BusinessException(
					"Unable to reach Gemini chat endpoint.",
					HttpStatus.BAD_GATEWAY
			);
		}
	}

	private static String extractText(GeminiGenerateResponse response) {
		if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
			return null;
		}
		GeminiGenerateResponse.Candidate first = response.candidates().get(0);
		if (first == null || first.content() == null || first.content().parts() == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (GeminiGenerateResponse.Part part : first.content().parts()) {
			if (part != null && part.text() != null && !part.text().isBlank()) {
				if (!sb.isEmpty()) {
					sb.append('\n');
				}
				sb.append(part.text());
			}
		}
		return sb.toString();
	}

	public record GeminiGenerateResponse(List<Candidate> candidates) {
		public record Candidate(Content content) {
		}

		public record Content(List<Part> parts) {
		}

		public record Part(String text) {
		}
	}
}
