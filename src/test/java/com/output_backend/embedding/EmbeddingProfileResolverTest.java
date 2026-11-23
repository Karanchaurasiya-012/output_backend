package com.output_backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import com.output_backend.ingestion.DocumentAccessService;
import com.output_backend.ingestion.DocumentEntity;
import com.output_backend.ingestion.DocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingProfileResolverTest {

	@Mock
	private DocumentAccessService documentAccessService;

	@Mock
	private DocumentEntity docA;

	@Mock
	private DocumentEntity docB;

	private AppProperties appProperties;
	private EmbeddingProfileResolver resolver;

	@BeforeEach
	void setUp() {
		appProperties = new AppProperties();
		appProperties.getEmbedding().setDefaultProvider("ollama");
		appProperties.getEmbedding().setDimensions(1024);
		appProperties.getEmbedding().getOllama().setModel("bge-m3:latest");
		resolver = new EmbeddingProfileResolver(documentAccessService, appProperties);
	}

	@Test
	void resolvesSingleProfileFromCompletedDocs() {
		UUID owner = UUID.randomUUID();
		UUID id = UUID.randomUUID();
		when(docA.getStatus()).thenReturn(DocumentStatus.COMPLETED);
		when(docA.getEmbeddingProvider()).thenReturn("ollama");
		when(docA.getEmbeddingModel()).thenReturn("bge-m3:latest");
		when(docA.getEmbeddingDimensions()).thenReturn(1024);
		when(documentAccessService.requireOwnedDocuments(List.of(id), owner)).thenReturn(List.of(docA));

		EmbeddingProfile profile = resolver.resolve(owner, List.of(id));
		assertThat(profile.compatibilityKey()).isEqualTo("ollama|bge-m3:latest|1024");
	}

	@Test
	void rejectsMixedProfiles() {
		UUID owner = UUID.randomUUID();
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		when(docA.getStatus()).thenReturn(DocumentStatus.COMPLETED);
		when(docA.getEmbeddingProvider()).thenReturn("ollama");
		when(docA.getEmbeddingModel()).thenReturn("bge-m3:latest");
		when(docA.getEmbeddingDimensions()).thenReturn(1024);

		when(docB.getStatus()).thenReturn(DocumentStatus.COMPLETED);
		when(docB.getEmbeddingProvider()).thenReturn("gemini");
		when(docB.getEmbeddingModel()).thenReturn("gemini-embedding-001");
		when(docB.getEmbeddingDimensions()).thenReturn(1024);

		when(documentAccessService.requireOwnedDocuments(List.of(a, b), owner)).thenReturn(List.of(docA, docB));

		assertThatThrownBy(() -> resolver.resolve(owner, List.of(a, b)))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("incompatible embedding profiles");
	}

	@Test
	void defaultProfileUsesConfiguredProvider() {
		EmbeddingProfile profile = resolver.defaultProfile();
		assertThat(profile.provider()).isEqualTo(EmbeddingProviderId.OLLAMA);
		assertThat(profile.model()).isEqualTo("bge-m3:latest");
		assertThat(profile.dimensions()).isEqualTo(1024);
	}
}
