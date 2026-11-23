package com.output_backend.llm;

import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

import com.output_backend.cache.AnswerResultCache;
import com.output_backend.cache.DocumentIndexVersionService;
import com.output_backend.cache.QueryHash;
import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import com.output_backend.context.BuiltContext;
import com.output_backend.context.ContextPreparationService;
import com.output_backend.context.ContextSource;
import com.output_backend.embedding.EmbeddingProfile;
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
	private final AnswerResultCache answerResultCache;
	private final DocumentIndexVersionService documentIndexVersionService;

	public AnswerGenerationService(
			ContextPreparationService contextPreparationService,
			PromptBuilder promptBuilder,
			SourceCitationValidator sourceCitationValidator,
			List<LlmProvider> providerList,
			AppProperties appProperties,
			AnswerResultCache answerResultCache,
			DocumentIndexVersionService documentIndexVersionService) {
		this.contextPreparationService = contextPreparationService;
		this.promptBuilder = promptBuilder;
		this.sourceCitationValidator = sourceCitationValidator;
		Map<LlmProviderId, LlmProvider> map = new EnumMap<>(LlmProviderId.class);
		for (LlmProvider provider : providerList) {
			map.put(provider.id(), provider);
		}
		this.providers = Map.copyOf(map);
		this.appProperties = appProperties;
		this.answerResultCache = answerResultCache;
		this.documentIndexVersionService = documentIndexVersionService;
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
		LlmProviderId providerId = resolveProviderId(llmProviderOverride);

		if (context.empty()) {
			return AnswerResult.noContext(prepared, providerId, false);
		}

		LlmProvider provider = requireProvider(providerId);
		EmbeddingProfile profile = prepared.retrieval().profile();
		String queryHash = QueryHash.ofQuestion(question);
		String scopeHash = QueryHash.ofDocumentScope(documentIds);
		String docVersion = documentIndexVersionService.currentVersion(ownerUserId);
		String chunkIdsHash = chunkIdsHash(context.sources());

		var cached = answerResultCache.get(
				ownerUserId,
				scopeHash,
				profile,
				queryHash,
				docVersion,
				chunkIdsHash,
				provider.id().name().toLowerCase(Locale.ROOT),
				provider.model(),
				PromptBuilder.PROMPT_VERSION
		);
		if (cached.isPresent()) {
			AnswerResultCache.CachedAnswer hit = cached.get();
			List<ContextSource> citations = hit.citations() == null ? List.of() : hit.citations().stream()
					.map(c -> new ContextSource(
							c.sourceKey(),
							c.chunkId(),
							c.documentId(),
							c.documentName(),
							c.pageStart(),
							c.pageEnd(),
							0,
							c.similarity(),
							0
					))
					.toList();
			return new AnswerResult(
					hit.grounded(),
					hit.answer(),
					citations,
					hit.rejectedSourceKeys() == null ? List.of() : hit.rejectedSourceKeys(),
					provider.id().name().toLowerCase(Locale.ROOT),
					provider.model(),
					PromptBuilder.PROMPT_VERSION,
					prepared,
					hit.noContext(),
					true
			);
		}

		String userPrompt = promptBuilder.buildUserPrompt(question, context);
		LlmGenerationResult generation = provider.generate(PromptBuilder.SYSTEM_PROMPT, userPrompt);
		SourceCitationValidator.ValidatedAnswer validated =
				sourceCitationValidator.validate(generation.rawText(), context);

		AnswerResult result = new AnswerResult(
				true,
				validated.answer(),
				validated.citations(),
				validated.rejectedSourceKeys(),
				provider.id().name().toLowerCase(Locale.ROOT),
				provider.model(),
				PromptBuilder.PROMPT_VERSION,
				prepared,
				false,
				false
		);

		answerResultCache.put(
				ownerUserId,
				scopeHash,
				profile,
				queryHash,
				docVersion,
				chunkIdsHash,
				result.llmProvider(),
				result.llmModel(),
				result.promptVersion(),
				new AnswerResultCache.CachedAnswer(
						result.answer(),
						result.grounded(),
						result.noContext(),
						result.citations().stream()
								.map(c -> new AnswerResultCache.CachedCitation(
										c.sourceKey(),
										c.chunkId(),
										c.documentId(),
										c.documentName(),
										c.pageStart(),
										c.pageEnd(),
										c.similarity()
								))
								.toList(),
						result.rejectedSourceKeys()
				)
		);

		return result;
	}

	private static String chunkIdsHash(List<ContextSource> sources) {
		String joined = sources.stream()
				.map(s -> s.chunkId().toString())
				.collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), set -> String.join(",", set)));
		return QueryHash.sha256(joined);
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
			boolean noContext,
			boolean answerCacheHit
	) {
		static AnswerResult noContext(
				ContextPreparationService.ContextPreparationResult preparation,
				LlmProviderId requestedProvider,
				boolean answerCacheHit
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
					true,
					answerCacheHit
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
			data.put("answerCacheHit", answerCacheHit);
			data.put("embeddingCacheHit", preparation.retrieval().embeddingCacheHit());
			data.put("retrievalCacheHit", preparation.retrieval().retrievalCacheHit());
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
