package com.output_backend.context;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Structured LLM context plus citation map keyed by sourceKey (e.g. S1).
 */
public record BuiltContext(
		String renderedContext,
		List<ContextSource> sources,
		Map<UUID, ContextSource> sourcesByChunkId,
		int estimatedTokens,
		int tokenBudget,
		int inputChunkCount,
		int duplicateChunkIdCount,
		int duplicateContentCount,
		int droppedForBudgetCount,
		boolean empty
) {
}
