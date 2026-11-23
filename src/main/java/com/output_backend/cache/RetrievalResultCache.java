package com.output_backend.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.output_backend.config.AppProperties;
import com.output_backend.embedding.EmbeddingProfile;
import com.output_backend.retrieval.RetrievedChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Caches ranked retrieval hits (including excerpts/content needed for context).
 * Keys are user-scoped and versioned; callers must still enforce ownership on use.
 */
@Service
public class RetrievalResultCache {

	private static final Logger log = LoggerFactory.getLogger(RetrievalResultCache.class);

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final AppProperties appProperties;

	public RetrievalResultCache(
			StringRedisTemplate redis,
			ObjectMapper objectMapper,
			AppProperties appProperties) {
		this.redis = redis;
		this.objectMapper = objectMapper;
		this.appProperties = appProperties;
	}

	public Optional<CachedRetrieval> get(
			UUID ownerUserId,
			String documentScopeHash,
			EmbeddingProfile profile,
			String queryHash,
			String documentIndexVersion,
			int topK,
			double threshold
	) {
		try {
			String key = CacheKeys.retrieval(
					ownerUserId, documentScopeHash, profile, queryHash, documentIndexVersion, topK, threshold);
			String json = redis.opsForValue().get(key);
			if (json == null || json.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(objectMapper.readValue(json, CachedRetrieval.class));
		}
		catch (Exception ex) {
			log.warn("Retrieval cache get failed. type={}", ex.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	public void put(
			UUID ownerUserId,
			String documentScopeHash,
			EmbeddingProfile profile,
			String queryHash,
			String documentIndexVersion,
			int topK,
			double threshold,
			CachedRetrieval value
	) {
		try {
			String key = CacheKeys.retrieval(
					ownerUserId, documentScopeHash, profile, queryHash, documentIndexVersion, topK, threshold);
			String json = objectMapper.writeValueAsString(value);
			Duration ttl = Duration.ofSeconds(Math.max(1, appProperties.getRedisCache().getRetrievalTtlSeconds()));
			redis.opsForValue().set(key, json, ttl);
		}
		catch (Exception ex) {
			log.warn("Retrieval cache put failed. type={}", ex.getClass().getSimpleName());
		}
	}

	public record CachedRetrieval(int candidatesConsidered, List<RetrievedChunk> chunks) {
	}
}
