package com.output_backend.cache;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Per-user document index version included in retrieval/answer cache keys.
 * Bump when documents change so stale retrieval/answer entries miss naturally.
 */
@Service
public class DocumentIndexVersionService {

	private static final Logger log = LoggerFactory.getLogger(DocumentIndexVersionService.class);

	private final StringRedisTemplate redis;

	public DocumentIndexVersionService(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public String currentVersion(UUID ownerUserId) {
		try {
			String key = CacheKeys.documentIndexVersion(ownerUserId);
			String value = redis.opsForValue().get(key);
			return value == null || value.isBlank() ? "0" : value.trim();
		}
		catch (Exception ex) {
			log.warn("Redis doc-version read failed; using 0. type={}", ex.getClass().getSimpleName());
			return "0";
		}
	}

	public String bump(UUID ownerUserId) {
		try {
			String key = CacheKeys.documentIndexVersion(ownerUserId);
			Long next = redis.opsForValue().increment(key);
			redis.expire(key, 30, TimeUnit.DAYS);
			return next == null ? "0" : Long.toString(next);
		}
		catch (Exception ex) {
			log.warn("Redis doc-version bump failed. type={}", ex.getClass().getSimpleName());
			return currentVersion(ownerUserId);
		}
	}
}
