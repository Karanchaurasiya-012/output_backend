package com.output_backend.embedding;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Resolves document embedding profile, selects the matching provider (no silent fallback),
 * and embeds the query.
 */
@Service
public class QueryEmbeddingService {

	private final Map<EmbeddingProviderId, QueryEmbeddingProvider> providers;
	private final EmbeddingProfileResolver profileResolver;

	public QueryEmbeddingService(
			List<QueryEmbeddingProvider> providerList,
			EmbeddingProfileResolver profileResolver) {
		Map<EmbeddingProviderId, QueryEmbeddingProvider> map = new EnumMap<>(EmbeddingProviderId.class);
		for (QueryEmbeddingProvider provider : providerList) {
			map.put(provider.id(), provider);
		}
		this.providers = Map.copyOf(map);
		this.profileResolver = profileResolver;
	}

	public QueryEmbeddingResult embedForDocuments(UUID ownerUserId, String question, Collection<UUID> documentIds) {
		EmbeddingProfile profile = profileResolver.resolve(ownerUserId, documentIds);
		QueryEmbeddingProvider provider = requireProvider(profile.provider());
		profileResolver.assertProviderModelMatch(profile, provider);
		QueryEmbeddingVector vector = provider.embedQuery(question);
		if (!profile.model().equals(vector.model())
				|| !profile.providerKey().equals(vector.provider())
				|| profile.dimensions() != vector.dimensions()) {
			throw new BusinessException(
					"Embedded query metadata does not match the document embedding profile.",
					HttpStatus.BAD_GATEWAY
			);
		}
		return new QueryEmbeddingResult(profile, vector);
	}

	public QueryEmbeddingVector embedWithProvider(String providerName, String question) {
		EmbeddingProviderId id;
		try {
			id = EmbeddingProviderId.from(providerName);
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException("Unknown embedding provider: " + providerName, HttpStatus.BAD_REQUEST);
		}
		return requireProvider(id).embedQuery(question);
	}

	private QueryEmbeddingProvider requireProvider(EmbeddingProviderId id) {
		QueryEmbeddingProvider provider = providers.get(id);
		if (provider == null) {
			throw new BusinessException(
					"Embedding provider is not available: " + id.name().toLowerCase(),
					HttpStatus.NOT_IMPLEMENTED
			);
		}
		return provider;
	}

	public record QueryEmbeddingResult(EmbeddingProfile profile, QueryEmbeddingVector vector) {
	}
}
