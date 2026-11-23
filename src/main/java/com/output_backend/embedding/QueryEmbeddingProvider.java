package com.output_backend.embedding;

/**
 * Query-side embedding client. Implementations must not silently fall back to another provider.
 */
public interface QueryEmbeddingProvider {

	EmbeddingProviderId id();

	String model();

	QueryEmbeddingVector embedQuery(String text);
}
