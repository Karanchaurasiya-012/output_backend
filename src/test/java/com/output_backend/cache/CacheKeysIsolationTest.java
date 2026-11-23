package com.output_backend.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.output_backend.embedding.EmbeddingProfile;
import com.output_backend.embedding.EmbeddingProviderId;
import org.junit.jupiter.api.Test;

class CacheKeysIsolationTest {

	private final EmbeddingProfile profile =
			new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 1024);

	@Test
	void embeddingKeysDifferByOwner() {
		UUID ownerA = UUID.randomUUID();
		UUID ownerB = UUID.randomUUID();
		String queryHash = QueryHash.ofQuestion("notice period");

		String keyA = CacheKeys.queryEmbedding(ownerA, profile, queryHash);
		String keyB = CacheKeys.queryEmbedding(ownerB, profile, queryHash);

		assertThat(keyA).contains(ownerA.toString());
		assertThat(keyB).contains(ownerB.toString());
		assertThat(keyA).isNotEqualTo(keyB);
	}

	@Test
	void retrievalKeysDifferByOwnerEvenWithSameQueryAndScope() {
		UUID ownerA = UUID.randomUUID();
		UUID ownerB = UUID.randomUUID();
		String scope = QueryHash.ofDocumentScope(java.util.List.of());
		String queryHash = QueryHash.ofQuestion("same question");

		String keyA = CacheKeys.retrieval(ownerA, scope, profile, queryHash, "1", 5, 0.70);
		String keyB = CacheKeys.retrieval(ownerB, scope, profile, queryHash, "1", 5, 0.70);

		assertThat(keyA).isNotEqualTo(keyB);
	}

	@Test
	void answerKeysDifferByOwnerAndIncludePromptVersion() {
		UUID ownerA = UUID.randomUUID();
		UUID ownerB = UUID.randomUUID();
		String scope = "all";
		String queryHash = QueryHash.ofQuestion("q");
		String chunkHash = QueryHash.sha256("chunk");

		String keyA = CacheKeys.answer(
				ownerA, scope, profile, queryHash, "2", chunkHash, "ollama", "qwen3.5:4b", "rag-grounded-v2");
		String keyB = CacheKeys.answer(
				ownerB, scope, profile, queryHash, "2", chunkHash, "ollama", "qwen3.5:4b", "rag-grounded-v2");

		assertThat(keyA).contains("rag-grounded-v2");
		assertThat(keyA).isNotEqualTo(keyB);
	}

	@Test
	void documentIndexVersionKeysAreOwnerScoped() {
		UUID ownerA = UUID.randomUUID();
		UUID ownerB = UUID.randomUUID();

		assertThat(CacheKeys.documentIndexVersion(ownerA))
				.isNotEqualTo(CacheKeys.documentIndexVersion(ownerB));
		assertThat(CacheKeys.documentIndexVersion(ownerA)).startsWith("rag:docver:");
	}

	@Test
	void embeddingKeysDifferByProfileCompatibility() {
		UUID owner = UUID.randomUUID();
		String queryHash = QueryHash.ofQuestion("q");
		EmbeddingProfile ollama = new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 1024);
		EmbeddingProfile gemini = new EmbeddingProfile(EmbeddingProviderId.GEMINI, "gemini-embedding-001", 1024);

		assertThat(CacheKeys.queryEmbedding(owner, ollama, queryHash))
				.isNotEqualTo(CacheKeys.queryEmbedding(owner, gemini, queryHash));
	}
}
