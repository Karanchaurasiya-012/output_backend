package com.output_backend.retrieval;

import java.util.UUID;

/**
 * One owner-scoped chunk hit. Scoring convention:
 * {@code cosineDistance} = pgvector {@code <=>};
 * {@code similarity} = {@code 1 - cosineDistance} (higher is better).
 */
public record RetrievedChunk(
		UUID chunkId,
		UUID documentId,
		String documentName,
		int chunkIndex,
		String content,
		Integer pageStart,
		Integer pageEnd,
		String embeddingProvider,
		String embeddingModel,
		double cosineDistance,
		double similarity
) {
}
