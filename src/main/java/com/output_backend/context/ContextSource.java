package com.output_backend.context;

import java.util.UUID;

/**
 * One citation-ready source retained for later LLM answer validation.
 */
public record ContextSource(
		String sourceKey,
		UUID chunkId,
		UUID documentId,
		String documentName,
		Integer pageStart,
		Integer pageEnd,
		int chunkIndex,
		double similarity,
		int estimatedTokens
) {
}
