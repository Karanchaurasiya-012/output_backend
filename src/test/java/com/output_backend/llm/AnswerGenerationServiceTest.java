package com.output_backend.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import com.output_backend.context.BuiltContext;
import com.output_backend.context.ContextPreparationService;
import com.output_backend.context.ContextSource;
import com.output_backend.embedding.EmbeddingProfile;
import com.output_backend.embedding.EmbeddingProviderId;
import com.output_backend.retrieval.SimilaritySearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnswerGenerationServiceTest {

	@Mock
	private ContextPreparationService contextPreparationService;

	@Mock
	private LlmProvider ollama;

	private AnswerGenerationService service;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties();
		props.getLlm().setDefaultProvider("ollama");
		lenient().when(ollama.id()).thenReturn(LlmProviderId.OLLAMA);
		lenient().when(ollama.model()).thenReturn("qwen3.5:4b");

		service = new AnswerGenerationService(
				contextPreparationService,
				new PromptBuilder(),
				new SourceCitationValidator(),
				List.of(ollama),
				props
		);
	}

	@Test
	void skipsLlmWhenContextEmpty() {
		UUID owner = UUID.randomUUID();
		when(contextPreparationService.prepare(eq(owner), eq("q"), any()))
				.thenReturn(emptyPreparation());

		AnswerGenerationService.AnswerResult result = service.answer(owner, "q", List.of(), "ollama");

		assertThat(result.noContext()).isTrue();
		assertThat(result.answer()).contains("could not find");
		verify(ollama, never()).generate(any(), any());
	}

	@Test
	void generatesGroundedAnswerWithValidatedCitations() {
		UUID owner = UUID.randomUUID();
		UUID chunkId = UUID.randomUUID();
		ContextSource s1 = new ContextSource("S1", chunkId, UUID.randomUUID(), "a.pdf", 4, 4, 0, 0.9, 20);
		BuiltContext context = new BuiltContext(
				"[S1] Document: a.pdf | page 4 | chunkId=" + chunkId + "\nThe notice period is 30 days.\n",
				List.of(s1),
				Map.of(chunkId, s1),
				30,
				1800,
				1,
				0,
				0,
				0,
				false
		);
		when(contextPreparationService.prepare(eq(owner), eq("notice?"), any()))
				.thenReturn(preparationWith(context));
		when(ollama.generate(any(), any())).thenReturn(
				new LlmGenerationResult("The notice period is 30 days.\nSOURCES: S1", "ollama", "qwen3.5:4b"));

		AnswerGenerationService.AnswerResult result = service.answer(owner, "notice?", List.of(), null);

		assertThat(result.noContext()).isFalse();
		assertThat(result.grounded()).isTrue();
		assertThat(result.citations()).containsExactly(s1);
		assertThat(result.llmModel()).isEqualTo("qwen3.5:4b");
	}

	@Test
	void doesNotFallbackWhenGeminiRequestedButUnavailable() {
		UUID owner = UUID.randomUUID();
		UUID chunkId = UUID.randomUUID();
		ContextSource s1 = new ContextSource("S1", chunkId, UUID.randomUUID(), "a.pdf", 1, 1, 0, 0.9, 10);
		BuiltContext context = new BuiltContext(
				"[S1]\nhello",
				List.of(s1),
				Map.of(chunkId, s1),
				10,
				1800,
				1,
				0,
				0,
				0,
				false
		);
		when(contextPreparationService.prepare(eq(owner), eq("q"), any()))
				.thenReturn(preparationWith(context));

		assertThatThrownBy(() -> service.answer(owner, "q", List.of(), "gemini"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("not available");
		verify(ollama, never()).generate(any(), any());
	}

	private static ContextPreparationService.ContextPreparationResult emptyPreparation() {
		return preparationWith(new BuiltContext("", List.of(), Map.of(), 0, 1800, 0, 0, 0, 0, true));
	}

	private static ContextPreparationService.ContextPreparationResult preparationWith(BuiltContext context) {
		SimilaritySearchService.SimilaritySearchResult retrieval = new SimilaritySearchService.SimilaritySearchResult(
				new EmbeddingProfile(EmbeddingProviderId.OLLAMA, "bge-m3:latest", 1024),
				5,
				0.70,
				0,
				List.of()
		);
		return new ContextPreparationService.ContextPreparationResult(retrieval, context);
	}
}
