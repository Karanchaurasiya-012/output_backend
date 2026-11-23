package com.output_backend.embedding;

import java.util.Locale;
import java.util.Objects;

/**
 * Compatibility key for query ↔ document vector space: (provider, model, dimensions).
 */
public record EmbeddingProfile(
		EmbeddingProviderId provider,
		String model,
		int dimensions
) {
	public EmbeddingProfile {
		Objects.requireNonNull(provider, "provider");
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("model required");
		}
		model = model.trim();
		if (dimensions <= 0) {
			throw new IllegalArgumentException("dimensions must be positive");
		}
	}

	public String providerKey() {
		return provider.name().toLowerCase(Locale.ROOT);
	}

	public String compatibilityKey() {
		return providerKey() + "|" + model + "|" + dimensions;
	}
}
