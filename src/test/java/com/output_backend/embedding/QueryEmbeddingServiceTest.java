package com.output_backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.output_backend.cache.QueryEmbeddingCache;
import com.output_backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryEmbeddingServiceTest {

	@Mock
	private EmbeddingProfileResolver profileResolver;

	@Mock
	private QueryEmbeddingCache queryEmbeddingCache;

	@Test
	void usesProfileProviderWithoutFallback() {
		RecordingProvider ollama = new RecordingProvider(EmbeddingProviderId.OLLAMA, "bge-m3:latest");
		RecordingProvider gemini = new RecordingProvider(EmbeddingProviderId.GEMINI, "gemini-embedding-001");
		EmbeddingProfile profile = new EmbeddingProfile(EmbeddingProviderId.GEMINI, "gemini-embedding-001", 2);
		UUID owner = UUID.randomUUID();

		when(profileResolver.resolve(eq(owner), any())).thenReturn(profile);
		when(queryEmbeddingCache.get(eq(owner), eq(profile), any())).thenReturn(Optional.empty());

		QueryEmbeddingService service = new QueryEmbeddingService(List.of(ollama, gemini), profileResolver, queryEmbeddingCache);
		QueryEmbeddingService.QueryEmbeddingResult result = service.embedForDocuments(owner, "hello", List.of());

		assertThat(result.vector().provider()).isEqualTo("gemini");
		assertThat(result.cacheHit()).isFalse();
		assertThat(gemini.calls).containsExactly("hello");
		assertThat(ollama.calls).isEmpty();
		verify(profileResolver).assertProviderModelMatch(profile, gemini);
		verify(queryEmbeddingCache).put(eq(owner), eq(profile), any(), any());
	}

	@Test
	void returnsCachedEmbeddingWithoutCallingProvider() {
		RecordingProvider ollama = new RecordingProvider(EmbeddingProviderId.OLLAMA, "bge-m3:latest");
		EmbeddingProfile profile = new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 2);
		UUID owner = UUID.randomUUID();
		QueryEmbeddingVector cached = new QueryEmbeddingVector(new float[]{0.3f, 0.4f}, "ollama", "bge-m3:latest", 2);

		when(profileResolver.resolve(eq(owner), any())).thenReturn(profile);
		when(queryEmbeddingCache.get(eq(owner), eq(profile), any())).thenReturn(Optional.of(cached));

		QueryEmbeddingService service = new QueryEmbeddingService(List.of(ollama), profileResolver, queryEmbeddingCache);
		QueryEmbeddingService.QueryEmbeddingResult result = service.embedForDocuments(owner, "hello", List.of());

		assertThat(result.cacheHit()).isTrue();
		assertThat(result.vector().values()).containsExactly(0.3f, 0.4f);
		assertThat(ollama.calls).isEmpty();
	}

	@Test
	void embedWithProviderDoesNotFallback() {
		RecordingProvider ollama = new RecordingProvider(EmbeddingProviderId.OLLAMA, "bge-m3:latest");
		QueryEmbeddingService service = new QueryEmbeddingService(List.of(ollama), profileResolver, queryEmbeddingCache);

		assertThatThrownBy(() -> service.embedWithProvider("gemini", "q"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("not available");
		assertThat(ollama.calls).isEmpty();
		verifyNoInteractions(profileResolver);
	}

	private static final class RecordingProvider implements QueryEmbeddingProvider {
		private final EmbeddingProviderId id;
		private final String model;
		private final java.util.List<String> calls = new java.util.ArrayList<>();

		private RecordingProvider(EmbeddingProviderId id, String model) {
			this.id = id;
			this.model = model;
		}

		@Override
		public EmbeddingProviderId id() {
			return id;
		}

		@Override
		public String model() {
			return model;
		}

		@Override
		public QueryEmbeddingVector embedQuery(String text) {
			calls.add(text);
			return new QueryEmbeddingVector(new float[]{0.1f, 0.2f}, id.name().toLowerCase(), model, 2);
		}
	}
}
