package com.output_backend.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.output_backend.config.AppProperties;
import com.output_backend.embedding.EmbeddingProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AnswerResultCache {

	private static final Logger log = LoggerFactory.getLogger(AnswerResultCache.class);

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final AppProperties appProperties;

	public AnswerResultCache(
			StringRedisTemplate redis,
			ObjectMapper objectMapper,
			AppProperties appProperties) {
		this.redis = redis;
		this.objectMapper = objectMapper;
		this.appProperties = appProperties;
	}

	public boolean enabled() {
		return appProperties.getRedisCache().isAnswerCacheEnabled();
	}

	public Optional<CachedAnswer> get(
			UUID ownerUserId,
			String documentScopeHash,
			EmbeddingProfile profile,
			String queryHash,
			String documentIndexVersion,
			String chunkIdsHash,
			String llmProvider,
			String llmModel,
			String promptVersion
	) {
		if (!enabled()) {
			return Optional.empty();
		}
		try {
			String key = CacheKeys.answer(
					ownerUserId, documentScopeHash, profile, queryHash, documentIndexVersion,
					chunkIdsHash, llmProvider, llmModel, promptVersion);
			String json = redis.opsForValue().get(key);
			if (json == null || json.isBlank()) {
				return Optional.empty();
			}
			return Optional.of(objectMapper.readValue(json, CachedAnswer.class));
		}
		catch (Exception ex) {
			log.warn("Answer cache get failed. type={}", ex.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	public void put(
			UUID ownerUserId,
			String documentScopeHash,
			EmbeddingProfile profile,
			String queryHash,
			String documentIndexVersion,
			String chunkIdsHash,
			String llmProvider,
			String llmModel,
			String promptVersion,
			CachedAnswer value
	) {
		if (!enabled()) {
			return;
		}
		try {
			String key = CacheKeys.answer(
					ownerUserId, documentScopeHash, profile, queryHash, documentIndexVersion,
					chunkIdsHash, llmProvider, llmModel, promptVersion);
			String json = objectMapper.writeValueAsString(value);
			Duration ttl = Duration.ofSeconds(Math.max(1, appProperties.getRedisCache().getAnswerTtlSeconds()));
			redis.opsForValue().set(key, json, ttl);
		}
		catch (Exception ex) {
			log.warn("Answer cache put failed. type={}", ex.getClass().getSimpleName());
		}
	}

	public record CachedAnswer(
			String answer,
			boolean grounded,
			boolean noContext,
			List<CachedCitation> citations,
			List<String> rejectedSourceKeys
	) {
	}

	public record CachedCitation(
			String sourceKey,
			UUID chunkId,
			UUID documentId,
			String documentName,
			Integer pageStart,
			Integer pageEnd,
			double similarity
	) {
	}
}
