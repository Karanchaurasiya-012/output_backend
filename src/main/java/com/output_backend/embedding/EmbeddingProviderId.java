package com.output_backend.embedding;

import java.util.Locale;

public enum EmbeddingProviderId {
	OLLAMA,
	GEMINI;

	public static EmbeddingProviderId from(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new IllegalArgumentException("embedding provider is required");
		}
		return EmbeddingProviderId.valueOf(raw.trim().toUpperCase(Locale.ROOT));
	}
}
