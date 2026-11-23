package com.output_backend.cache;

import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * Explicit invalidation: bump per-user document index version so retrieval/answer keys miss.
 * Embedding cache remains query-hash scoped and expires via TTL.
 */
@Service
public class RagCacheInvalidationService {

	private final DocumentIndexVersionService documentIndexVersionService;

	public RagCacheInvalidationService(DocumentIndexVersionService documentIndexVersionService) {
		this.documentIndexVersionService = documentIndexVersionService;
	}

	public String invalidateForOwner(UUID ownerUserId) {
		return documentIndexVersionService.bump(ownerUserId);
	}
}
