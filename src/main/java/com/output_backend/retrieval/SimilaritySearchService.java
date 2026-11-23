package com.output_backend.retrieval;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import com.output_backend.embedding.EmbeddingProfile;
import com.output_backend.embedding.QueryEmbeddingService;
import com.output_backend.embedding.QueryEmbeddingVector;
import com.output_backend.ingestion.DocumentAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Embeds the query in the document profile space, runs Top-K pgvector search,
 * then applies the cosine-similarity threshold (default 0.70).
 */
@Service
public class SimilaritySearchService {

	private final QueryEmbeddingService queryEmbeddingService;
	private final ChunkSimilarityRepository chunkSimilarityRepository;
	private final DocumentAccessService documentAccessService;
	private final AppProperties appProperties;

	public SimilaritySearchService(
			QueryEmbeddingService queryEmbeddingService,
			ChunkSimilarityRepository chunkSimilarityRepository,
			DocumentAccessService documentAccessService,
			AppProperties appProperties) {
		this.queryEmbeddingService = queryEmbeddingService;
		this.chunkSimilarityRepository = chunkSimilarityRepository;
		this.documentAccessService = documentAccessService;
		this.appProperties = appProperties;
	}

	public SimilaritySearchResult search(UUID ownerUserId, String question, Collection<UUID> documentIds) {
		if (question == null || question.isBlank()) {
			throw new BusinessException("Question is required.", HttpStatus.BAD_REQUEST);
		}

		Collection<UUID> scopedDocumentIds = documentIds;
		if (documentIds != null && !documentIds.isEmpty()) {
			// Enforce ownership before retrieval SQL (defense in depth with SQL filters).
			documentAccessService.requireOwnedDocuments(documentIds, ownerUserId);
			scopedDocumentIds = documentIds;
		}

		QueryEmbeddingService.QueryEmbeddingResult embedded =
				queryEmbeddingService.embedForDocuments(ownerUserId, question, scopedDocumentIds);

		EmbeddingProfile profile = embedded.profile();
		QueryEmbeddingVector vector = embedded.vector();
		int topK = Math.max(1, appProperties.getRag().getTopK());
		double threshold = appProperties.getRag().getSimilarityThreshold();

		List<RetrievedChunk> topHits = chunkSimilarityRepository.findSimilar(
				ownerUserId,
				profile,
				vector.toPgVectorLiteral(),
				scopedDocumentIds,
				topK
		);

		List<RetrievedChunk> relevant = topHits.stream()
				.filter(hit -> hit.similarity() >= threshold)
				.toList();

		return new SimilaritySearchResult(
				profile,
				topK,
				threshold,
				topHits.size(),
				relevant
		);
	}

	public record SimilaritySearchResult(
			EmbeddingProfile profile,
			int topK,
			double similarityThreshold,
			int candidatesConsidered,
			List<RetrievedChunk> chunks
	) {
		public boolean hasRelevantContext() {
			return chunks != null && !chunks.isEmpty();
		}
	}
}
