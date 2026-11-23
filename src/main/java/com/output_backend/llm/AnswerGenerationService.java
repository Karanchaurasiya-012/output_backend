package com.output_backend.llm;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import com.output_backend.context.BuiltContext;
import com.output_backend.context.ContextPreparationService;
import com.output_backend.context.ContextSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AnswerGenerationService {

	private static final String NO_CONTEXT_ANSWER =
			"I could not find an answer in the selected documents.";

	private final ContextPreparationService contextPreparationService;
	private final PromptBuilder promptBuilder;
	private final SourceCitationValidator sourceCitationValidator;
	private final Map<LlmProviderId, LlmProvider> providers;
	private final AppProperties appProperties;

	public AnswerGenerationService(
			ContextPreparationService contextPreparationService,
			PromptBuilder promptBuilder,
			SourceCitationValidator sourceCitationValidator,
			List<LlmProvider> providerList,
			AppProperties appProperties) {
		this.contextPreparationService = contextPreparationService;
		this.promptBuilder = promptBuilder;
		this.sourceCitationValidator = sourceCitationValidator;
		Map<LlmProviderId, LlmProvider> map = new EnumMap<>(LlmProviderId.class);
		for (LlmProvider provider : providerList) {
			map.put(provider.id(), provider);
		}
		this.providers = Map.copyOf(map);
		this.appProperties = appProperties;
	}

	public AnswerResult answer(
			UUID ownerUserId,
			String question,
			Collection<UUID> documentIds,
			String llmProviderOverride
	) {
		ContextPreparationService.ContextPreparationResult prepared =
				contextPreparationService.prepare(ownerUserId, question, documentIds);
		BuiltContext context = prepared.context();

		if (context.empty()) {
			return AnswerResult.noContext(prepared, resolveProviderId(llmProviderOverride));
		}

		LlmProviderId providerId = resolveProviderId(llmProviderOverride);
		LlmProvider provider = requireProvider(providerId);

		String userPrompt = promptBuilder.buildUserPrompt(question, context);
		LlmGenerationResult generation = provider.generate(PromptBuilder.SYSTEM_PROMPT, userPrompt);
		SourceCitationValidator.ValidatedAnswer validated =
				sourceCitationValidator.validate(generation.rawText(), context);

		return new AnswerResult(
				true,
				validated.answer(),
				validated.citations(),
				validated.rejectedSourceKeys(),
				provider.id().name().toLowerCase(Locale.ROOT),
				provider.model(),
				PromptBuilder.PROMPT_VERSION,
				prepared,
				false
		);
	}

	private LlmProviderId resolveProviderId(String override) {
		String raw = override == null || override.isBlank()
				? appProperties.getLlm().getDefaultProvider()
				: override;
		try {
			return LlmProviderId.from(raw);
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException("Unknown LLM provider: " + raw, HttpStatus.BAD_REQUEST);
		}
	}

	private LlmProvider requireProvider(LlmProviderId id) {
		LlmProvider provider = providers.get(id);
		if (provider == null) {
			throw new BusinessException(
					"LLM provider is not available: " + id.name().toLowerCase(Locale.ROOT),
					HttpStatus.NOT_IMPLEMENTED
			);
		}
		return provider;
	}

	public record AnswerResult(
			boolean grounded,
			String answer,
			List<ContextSource> citations,
			List<String> rejectedSourceKeys,
			String llmProvider,
			String llmModel,
			String promptVersion,
			ContextPreparationService.ContextPreparationResult preparation,
			boolean noContext
	) {
		static AnswerResult noContext(
				ContextPreparationService.ContextPreparationResult preparation,
				LlmProviderId requestedProvider
		) {
			return new AnswerResult(
					false,
					NO_CONTEXT_ANSWER,
					List.of(),
					List.of(),
					requestedProvider.name().toLowerCase(Locale.ROOT),
					null,
					PromptBuilder.PROMPT_VERSION,
					preparation,
					true
			);
		}

		public Map<String, Object> toSafeView() {
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("answer", answer);
			data.put("grounded", grounded);
			data.put("noContext", noContext);
			data.put("llmProvider", llmProvider);
			data.put("llmModel", llmModel);
			data.put("promptVersion", promptVersion);
			data.put("rejectedSourceKeys", rejectedSourceKeys);
			data.put("citations", citations.stream().map(c -> {
				Map<String, Object> row = new LinkedHashMap<>();
				row.put("sourceKey", c.sourceKey());
				row.put("chunkId", c.chunkId().toString());
				row.put("documentId", c.documentId().toString());
				row.put("documentName", c.documentName());
				row.put("pageStart", c.pageStart());
				row.put("pageEnd", c.pageEnd());
				row.put("similarity", c.similarity());
				return row;
			}).toList());
			data.put("retrieval", Map.of(
					"embeddingProvider", preparation.retrieval().profile().providerKey(),
					"embeddingModel", preparation.retrieval().profile().model(),
					"candidatesConsidered", preparation.retrieval().candidatesConsidered(),
					"sourceCount", preparation.context().sources().size()
			));
			return data;
		}
	}
}
