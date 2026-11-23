package com.output_backend.retrieval;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.output_backend.cache.DocumentIndexVersionService;
import com.output_backend.cache.QueryHash;
import com.output_backend.cache.RetrievalResultCache;
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
	private final RetrievalResultCache retrievalResultCache;
	private final DocumentIndexVersionService documentIndexVersionService;

	public SimilaritySearchService(
			QueryEmbeddingService queryEmbeddingService,
			ChunkSimilarityRepository chunkSimilarityRepository,
			DocumentAccessService documentAccessService,
			AppProperties appProperties,
			RetrievalResultCache retrievalResultCache,
			DocumentIndexVersionService documentIndexVersionService) {
		this.queryEmbeddingService = queryEmbeddingService;
		this.chunkSimilarityRepository = chunkSimilarityRepository;
		this.documentAccessService = documentAccessService;
		this.appProperties = appProperties;
		this.retrievalResultCache = retrievalResultCache;
		this.documentIndexVersionService = documentIndexVersionService;
	}

	public SimilaritySearchResult search(UUID ownerUserId, String question, Collection<UUID> documentIds) {
		if (question == null || question.isBlank()) {
			throw new BusinessException("Question is required.", HttpStatus.BAD_REQUEST);
		}

		Collection<UUID> scopedDocumentIds = documentIds;
		if (documentIds != null && !documentIds.isEmpty()) {
			documentAccessService.requireOwnedDocuments(documentIds, ownerUserId);
			scopedDocumentIds = documentIds;
		}

		QueryEmbeddingService.QueryEmbeddingResult embedded =
				queryEmbeddingService.embedForDocuments(ownerUserId, question, scopedDocumentIds);

		EmbeddingProfile profile = embedded.profile();
		QueryEmbeddingVector vector = embedded.vector();
		int topK = Math.max(1, appProperties.getRag().getTopK());
		double threshold = appProperties.getRag().getSimilarityThreshold();
		String queryHash = QueryHash.ofQuestion(question);
		String scopeHash = QueryHash.ofDocumentScope(scopedDocumentIds);
		String docVersion = documentIndexVersionService.currentVersion(ownerUserId);

		var cached = retrievalResultCache.get(
				ownerUserId, scopeHash, profile, queryHash, docVersion, topK, threshold);
		if (cached.isPresent()) {
			RetrievalResultCache.CachedRetrieval hit = cached.get();
			List<RetrievedChunk> relevant = hit.chunks() == null ? List.of() : hit.chunks();
			// Defense in depth: cached rows must still belong to this owner/profile.
			relevant = relevant.stream()
					.filter(c -> profile.providerKey().equals(c.embeddingProvider()))
					.filter(c -> profile.model().equals(c.embeddingModel()))
					.filter(c -> c.similarity() >= threshold)
					.toList();
			return new SimilaritySearchResult(
					profile, topK, threshold, hit.candidatesConsidered(), relevant,
					embedded.cacheHit(), true);
		}

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

		retrievalResultCache.put(
				ownerUserId,
				scopeHash,
				profile,
				queryHash,
				docVersion,
				topK,
				threshold,
				new RetrievalResultCache.CachedRetrieval(topHits.size(), relevant)
		);

		return new SimilaritySearchResult(
				profile, topK, threshold, topHits.size(), relevant,
				embedded.cacheHit(), false);
	}

	public record SimilaritySearchResult(
			EmbeddingProfile profile,
			int topK,
			double similarityThreshold,
			int candidatesConsidered,
			List<RetrievedChunk> chunks,
			boolean embeddingCacheHit,
			boolean retrievalCacheHit
	) {
		public SimilaritySearchResult(
				EmbeddingProfile profile,
				int topK,
				double similarityThreshold,
				int candidatesConsidered,
				List<RetrievedChunk> chunks
		) {
			this(profile, topK, similarityThreshold, candidatesConsidered, chunks, false, false);
		}

		public boolean hasRelevantContext() {
			return chunks != null && !chunks.isEmpty();
		}
	}
}
