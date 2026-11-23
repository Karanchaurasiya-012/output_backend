package com.output_backend.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.output_backend.config.AppProperties;
import com.output_backend.retrieval.RetrievedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContextBuilderServiceTest {

	private AppProperties appProperties;
	private ContextBuilderService service;

	@BeforeEach
	void setUp() {
		appProperties = new AppProperties();
		appProperties.getRag().setContextTokenBudget(80);
		service = new ContextBuilderService(new TokenEstimator(), appProperties);
	}

	@Test
	void removesDuplicateChunkIdsAndContent() {
		UUID sameId = UUID.randomUUID();
		UUID docId = UUID.randomUUID();
		RetrievedChunk a = chunk(sameId, docId, "Notice period is 30 days.", 0.91);
		RetrievedChunk dupId = chunk(sameId, docId, "Notice period is 30 days.", 0.90);
		RetrievedChunk dupContent = chunk(UUID.randomUUID(), docId, "  Notice   period is 30 days. ", 0.89);
		RetrievedChunk other = chunk(UUID.randomUUID(), docId, "Salary is paid monthly.", 0.85);

		BuiltContext context = service.build(List.of(a, dupId, dupContent, other));

		assertThat(context.duplicateChunkIdCount()).isEqualTo(1);
		assertThat(context.duplicateContentCount()).isEqualTo(1);
		assertThat(context.sources()).hasSizeGreaterThanOrEqualTo(1);
		assertThat(context.sourcesByChunkId()).containsKey(a.chunkId());
		assertThat(context.renderedContext()).contains("[S1]");
		assertThat(context.renderedContext()).contains("page 4");
		assertThat(context.empty()).isFalse();
	}

	@Test
	void enforcesTokenBudgetAndKeepsCitationMap() {
		appProperties.getRag().setContextTokenBudget(40);
		UUID docId = UUID.randomUUID();
		RetrievedChunk first = chunk(UUID.randomUUID(), docId, "Alpha beta gamma delta epsilon zeta eta theta.", 0.95);
		RetrievedChunk second = chunk(
				UUID.randomUUID(),
				docId,
				"One two three four five six seven eight nine ten eleven twelve thirteen.",
				0.90
		);

		BuiltContext context = service.build(List.of(first, second));

		assertThat(context.estimatedTokens()).isLessThanOrEqualTo(context.tokenBudget());
		assertThat(context.sources()).isNotEmpty();
		assertThat(context.sourcesByChunkId()).hasSize(context.sources().size());
		assertThat(context.droppedForBudgetCount() + context.sources().size())
				.isEqualTo(2);
	}

	@Test
	void emptyInputYieldsEmptyContext() {
		BuiltContext context = service.build(List.of());
		assertThat(context.empty()).isTrue();
		assertThat(context.renderedContext()).isEmpty();
		assertThat(context.sources()).isEmpty();
	}

	private static RetrievedChunk chunk(UUID chunkId, UUID documentId, String content, double similarity) {
		return new RetrievedChunk(
				chunkId,
				documentId,
				"Employment Agreement.pdf",
				0,
				content,
				4,
				4,
				"ollama",
				"bge-m3:latest",
				1.0 - similarity,
				similarity
		);
	}
}
