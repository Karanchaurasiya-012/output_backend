package com.output_backend.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.output_backend.cache.DocumentIndexVersionService;
import com.output_backend.cache.RetrievalResultCache;
import com.output_backend.config.AppProperties;
import com.output_backend.embedding.EmbeddingProfile;
import com.output_backend.embedding.EmbeddingProviderId;
import com.output_backend.embedding.QueryEmbeddingService;
import com.output_backend.embedding.QueryEmbeddingVector;
import com.output_backend.ingestion.DocumentAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SimilaritySearchServiceTest {

	@Mock
	private QueryEmbeddingService queryEmbeddingService;

	@Mock
	private ChunkSimilarityRepository chunkSimilarityRepository;

	@Mock
	private DocumentAccessService documentAccessService;

	@Mock
	private RetrievalResultCache retrievalResultCache;

	@Mock
	private DocumentIndexVersionService documentIndexVersionService;

	private AppProperties appProperties;
	private SimilaritySearchService service;

	@BeforeEach
	void setUp() {
		appProperties = new AppProperties();
		appProperties.getRag().setTopK(5);
		appProperties.getRag().setSimilarityThreshold(0.70);
		service = new SimilaritySearchService(
				queryEmbeddingService,
				chunkSimilarityRepository,
				documentAccessService,
				appProperties,
				retrievalResultCache,
				documentIndexVersionService
		);
	}

	@Test
	void filtersHitsBelowSimilarityThreshold() {
		UUID owner = UUID.randomUUID();
		EmbeddingProfile profile = new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 1024);
		float[] values = new float[1024];
		values[0] = 1f;
		QueryEmbeddingVector vector = new QueryEmbeddingVector(values, "ollama", "bge-m3:latest", 1024);

		when(queryEmbeddingService.embedForDocuments(eq(owner), eq("notice period"), any()))
				.thenReturn(new QueryEmbeddingService.QueryEmbeddingResult(profile, vector, false));
		when(documentIndexVersionService.currentVersion(owner)).thenReturn("0");
		when(retrievalResultCache.get(eq(owner), any(), eq(profile), any(), eq("0"), eq(5), eq(0.70)))
				.thenReturn(Optional.empty());

		RetrievedChunk keep = hit(0.20);
		RetrievedChunk drop = hit(0.40);
		when(chunkSimilarityRepository.findSimilar(eq(owner), eq(profile), any(), any(), eq(5)))
				.thenReturn(List.of(keep, drop));

		SimilaritySearchService.SimilaritySearchResult result =
				service.search(owner, "notice period", List.of());

		assertThat(result.candidatesConsidered()).isEqualTo(2);
		assertThat(result.chunks()).containsExactly(keep);
		assertThat(result.retrievalCacheHit()).isFalse();
		assertThat(result.hasRelevantContext()).isTrue();
		verify(retrievalResultCache).put(eq(owner), any(), eq(profile), any(), eq("0"), eq(5), eq(0.70), any());
	}

	@Test
	void usesRetrievalCacheHit() {
		UUID owner = UUID.randomUUID();
		EmbeddingProfile profile = new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 1024);
		float[] values = new float[1024];
		values[0] = 1f;
		QueryEmbeddingVector vector = new QueryEmbeddingVector(values, "ollama", "bge-m3:latest", 1024);
		RetrievedChunk keep = hit(0.10);

		when(queryEmbeddingService.embedForDocuments(eq(owner), eq("q"), any()))
				.thenReturn(new QueryEmbeddingService.QueryEmbeddingResult(profile, vector, true));
		when(documentIndexVersionService.currentVersion(owner)).thenReturn("3");
		when(retrievalResultCache.get(eq(owner), any(), eq(profile), any(), eq("3"), eq(5), eq(0.70)))
				.thenReturn(Optional.of(new RetrievalResultCache.CachedRetrieval(1, List.of(keep))));

		SimilaritySearchService.SimilaritySearchResult result = service.search(owner, "q", List.of());

		assertThat(result.retrievalCacheHit()).isTrue();
		assertThat(result.embeddingCacheHit()).isTrue();
		assertThat(result.chunks()).containsExactly(keep);
	}

	@Test
	void blocksSearchWhenDocumentNotOwned() {
		UUID owner = UUID.randomUUID();
		UUID foreignDoc = UUID.randomUUID();
		when(documentAccessService.requireOwnedDocuments(List.of(foreignDoc), owner))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more documents were not found"));

		assertThatThrownBy(() -> service.search(owner, "q", List.of(foreignDoc)))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("not found");
		verify(queryEmbeddingService, never()).embedForDocuments(any(), any(), any());
		verify(chunkSimilarityRepository, never()).findSimilar(any(), any(), any(), any(), anyInt());
	}

	@Test
	void filtersCachedChunksFromForeignEmbeddingProfile() {
		UUID owner = UUID.randomUUID();
		EmbeddingProfile profile = new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 1024);
		float[] values = new float[1024];
		values[0] = 1f;
		QueryEmbeddingVector vector = new QueryEmbeddingVector(values, "ollama", "bge-m3:latest", 1024);
		RetrievedChunk foreignProfile = new RetrievedChunk(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"doc.pdf",
				0,
				"content",
				1,
				1,
				"gemini",
				"gemini-embedding-001",
				0.10,
				0.90
		);

		when(queryEmbeddingService.embedForDocuments(eq(owner), eq("q"), any()))
				.thenReturn(new QueryEmbeddingService.QueryEmbeddingResult(profile, vector, true));
		when(documentIndexVersionService.currentVersion(owner)).thenReturn("1");
		when(retrievalResultCache.get(eq(owner), any(), eq(profile), any(), eq("1"), eq(5), eq(0.70)))
				.thenReturn(Optional.of(new RetrievalResultCache.CachedRetrieval(1, List.of(foreignProfile))));

		SimilaritySearchService.SimilaritySearchResult result = service.search(owner, "q", List.of());

		assertThat(result.retrievalCacheHit()).isTrue();
		assertThat(result.chunks()).isEmpty();
	}

	@Test
	void validatesOwnedDocumentIdsBeforeSearch() {
		UUID owner = UUID.randomUUID();
		UUID docId = UUID.randomUUID();
		EmbeddingProfile profile = new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 1024);
		float[] values = new float[1024];
		values[0] = 1f;
		QueryEmbeddingVector vector = new QueryEmbeddingVector(values, "ollama", "bge-m3:latest", 1024);

		when(documentAccessService.requireOwnedDocuments(List.of(docId), owner)).thenReturn(List.of());
		when(queryEmbeddingService.embedForDocuments(eq(owner), eq("q"), eq(List.of(docId))))
				.thenReturn(new QueryEmbeddingService.QueryEmbeddingResult(profile, vector, false));
		when(documentIndexVersionService.currentVersion(owner)).thenReturn("0");
		when(retrievalResultCache.get(any(), any(), any(), any(), any(), anyInt(), anyDouble()))
				.thenReturn(Optional.empty());
		when(chunkSimilarityRepository.findSimilar(eq(owner), eq(profile), any(), eq(List.of(docId)), eq(5)))
				.thenReturn(List.of());

		SimilaritySearchService.SimilaritySearchResult result = service.search(owner, "q", List.of(docId));

		assertThat(result.hasRelevantContext()).isFalse();
		verify(documentAccessService).requireOwnedDocuments(List.of(docId), owner);

		ArgumentCaptor<String> literal = ArgumentCaptor.forClass(String.class);
		verify(chunkSimilarityRepository).findSimilar(
				eq(owner), eq(profile), literal.capture(), eq(List.of(docId)), eq(5));
		assertThat(literal.getValue()).startsWith("[").endsWith("]");
	}

	private static RetrievedChunk hit(double distance) {
		return new RetrievedChunk(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"doc.pdf",
				0,
				"content",
				1,
				1,
				"ollama",
				"bge-m3:latest",
				distance,
				1.0 - distance
		);
	}
}
