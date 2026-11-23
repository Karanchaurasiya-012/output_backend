package com.output_backend.embedding;

/**
 * Gemini retrieval task types. Ollama/BGE-M3 ignores this hint.
 */
public enum EmbeddingTaskType {
	RETRIEVAL_DOCUMENT,
	RETRIEVAL_QUERY
}
