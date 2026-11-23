package com.output_backend.llm;

import java.util.Locale;

public enum LlmProviderId {
	OLLAMA,
	GEMINI;

	public static LlmProviderId from(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("llm provider is required");
		}
		return LlmProviderId.valueOf(raw.trim().toUpperCase(Locale.ROOT));
	}
}
