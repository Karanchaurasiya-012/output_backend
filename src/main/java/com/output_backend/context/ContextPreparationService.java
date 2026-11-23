package com.output_backend.context;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.retrieval.SimilaritySearchService;
import org.springframework.stereotype.Service;

/**
 * Phase-6 orchestration: retrieve relevant chunks, then build bounded context.
 */
@Service
public class ContextPreparationService {

	private final SimilaritySearchService similaritySearchService;
	private final ContextBuilderService contextBuilderService;

	public ContextPreparationService(
			SimilaritySearchService similaritySearchService,
			ContextBuilderService contextBuilderService) {
		this.similaritySearchService = similaritySearchService;
		this.contextBuilderService = contextBuilderService;
	}

	public ContextPreparationResult prepare(UUID ownerUserId, String question, Collection<UUID> documentIds) {
		SimilaritySearchService.SimilaritySearchResult retrieval =
				similaritySearchService.search(ownerUserId, question, documentIds);
		BuiltContext context = contextBuilderService.build(retrieval.chunks());
		return new ContextPreparationResult(retrieval, context);
	}

	public record ContextPreparationResult(
			SimilaritySearchService.SimilaritySearchResult retrieval,
			BuiltContext context
	) {
		public Map<String, Object> toSafeView() {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("hasRelevantContext", retrieval.hasRelevantContext() && !context.empty());
			data.put("embeddingProvider", retrieval.profile().providerKey());
			data.put("embeddingModel", retrieval.profile().model());
			data.put("topK", retrieval.topK());
			data.put("similarityThreshold", retrieval.similarityThreshold());
			data.put("candidatesConsidered", retrieval.candidatesConsidered());
			data.put("tokenBudget", context.tokenBudget());
			data.put("estimatedTokens", context.estimatedTokens());
			data.put("inputChunkCount", context.inputChunkCount());
			data.put("duplicateChunkIdCount", context.duplicateChunkIdCount());
			data.put("duplicateContentCount", context.duplicateContentCount());
			data.put("droppedForBudgetCount", context.droppedForBudgetCount());
			data.put("sourceCount", context.sources().size());
			data.put("sources", context.sources().stream().map(source -> {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("sourceKey", source.sourceKey());
				row.put("chunkId", source.chunkId().toString());
				row.put("documentId", source.documentId().toString());
				row.put("documentName", source.documentName());
				row.put("pageStart", source.pageStart());
				row.put("pageEnd", source.pageEnd());
				row.put("chunkIndex", source.chunkIndex());
				row.put("similarity", source.similarity());
				row.put("estimatedTokens", source.estimatedTokens());
				return row;
			}).toList());
			data.put("renderedContextPreview", preview(context.renderedContext(), 1200));
			return data;
		}

		private static String preview(String text, int maxChars) {
			if (text == null || text.isBlank()) {
				return "";
			}
			String trimmed = text.strip();
			return trimmed.length() <= maxChars ? trimmed : trimmed.substring(0, maxChars) + "…";
		}
	}
}
