package com.output_backend.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.output_backend.config.AppProperties;
import com.output_backend.embedding.EmbeddingProfile;
import com.output_backend.embedding.QueryEmbeddingVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class QueryEmbeddingCache {

	private static final Logger log = LoggerFactory.getLogger(QueryEmbeddingCache.class);

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;
	private final AppProperties appProperties;

	public QueryEmbeddingCache(
			StringRedisTemplate redis,
			ObjectMapper objectMapper,
			AppProperties appProperties) {
		this.redis = redis;
		this.objectMapper = objectMapper;
		this.appProperties = appProperties;
	}

	public Optional<QueryEmbeddingVector> get(UUID ownerUserId, EmbeddingProfile profile, String queryHash) {
		try {
			String json = redis.opsForValue().get(CacheKeys.queryEmbedding(ownerUserId, profile, queryHash));
			if (json == null || json.isBlank()) {
				return Optional.empty();
			}
			CachedEmbedding cached = objectMapper.readValue(json, CachedEmbedding.class);
			if (!profile.providerKey().equals(cached.provider())
					|| !profile.model().equals(cached.model())
					|| profile.dimensions() != cached.dimensions()
					|| cached.values() == null
					|| cached.values().length != profile.dimensions()) {
				return Optional.empty();
			}
			return Optional.of(new QueryEmbeddingVector(
					cached.values(), cached.provider(), cached.model(), cached.dimensions()));
		}
		catch (Exception ex) {
			log.warn("Embedding cache get failed. type={}", ex.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	public void put(UUID ownerUserId, EmbeddingProfile profile, String queryHash, QueryEmbeddingVector vector) {
		try {
			CachedEmbedding cached = new CachedEmbedding(
					vector.provider(), vector.model(), vector.dimensions(), vector.values());
			String json = objectMapper.writeValueAsString(cached);
			Duration ttl = Duration.ofSeconds(Math.max(1, appProperties.getRedisCache().getQueryEmbeddingTtlSeconds()));
			redis.opsForValue().set(CacheKeys.queryEmbedding(ownerUserId, profile, queryHash), json, ttl);
		}
		catch (Exception ex) {
			log.warn("Embedding cache put failed. type={}", ex.getClass().getSimpleName());
		}
	}

	public record CachedEmbedding(String provider, String model, int dimensions, float[] values) {
	}
}
