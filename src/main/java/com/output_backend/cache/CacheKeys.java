package com.output_backend.cache;

import java.util.UUID;

import com.output_backend.embedding.EmbeddingProfile;

public final class CacheKeys {

	private CacheKeys() {
	}

	public static String documentIndexVersion(UUID ownerUserId) {
		return "rag:docver:" + ownerUserId;
	}

	public static String queryEmbedding(UUID ownerUserId, EmbeddingProfile profile, String queryHash) {
		return "rag:embedding:" + ownerUserId + ":" + profile.compatibilityKey() + ":" + queryHash;
	}

	public static String retrieval(
			UUID ownerUserId,
			String documentScopeHash,
			EmbeddingProfile profile,
			String queryHash,
			String documentIndexVersion,
			int topK,
			double threshold
	) {
		return "rag:retrieval:"
				+ ownerUserId + ":"
				+ documentScopeHash + ":"
				+ profile.compatibilityKey() + ":"
				+ queryHash + ":"
				+ documentIndexVersion + ":"
				+ topK + ":"
				+ threshold;
	}

	public static String answer(
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
		return "rag:answer:"
				+ ownerUserId + ":"
				+ documentScopeHash + ":"
				+ profile.compatibilityKey() + ":"
				+ queryHash + ":"
				+ documentIndexVersion + ":"
				+ chunkIdsHash + ":"
				+ llmProvider + ":"
				+ llmModel + ":"
				+ promptVersion;
	}
}
