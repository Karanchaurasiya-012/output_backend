package com.output_backend.embedding;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import com.output_backend.ingestion.DocumentAccessService;
import com.output_backend.ingestion.DocumentEntity;
import com.output_backend.ingestion.DocumentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Resolves a single embedding profile from owner-scoped COMPLETED documents.
 * Mixing providers/models in one query is rejected.
 */
@Service
public class EmbeddingProfileResolver {

	private final DocumentAccessService documentAccessService;
	private final AppProperties appProperties;

	public EmbeddingProfileResolver(DocumentAccessService documentAccessService, AppProperties appProperties) {
		this.documentAccessService = documentAccessService;
		this.appProperties = appProperties;
	}

	public EmbeddingProfile resolve(UUID ownerUserId, Collection<UUID> documentIds) {
		List<DocumentEntity> documents;
		if (documentIds == null || documentIds.isEmpty()) {
			documents = documentAccessService.listCompletedForOwner(ownerUserId);
		}
		else {
			documents = documentAccessService.requireOwnedDocuments(documentIds, ownerUserId);
		}

		Set<String> keys = new LinkedHashSet<>();
		EmbeddingProfile chosen = null;
		for (DocumentEntity doc : documents) {
			if (doc.getStatus() != DocumentStatus.COMPLETED) {
				throw new BusinessException(
						"Document is not ready for retrieval: " + doc.getId(),
						HttpStatus.UNPROCESSABLE_ENTITY
				);
			}
			if (doc.getEmbeddingProvider() == null || doc.getEmbeddingProvider().isBlank()
					|| doc.getEmbeddingModel() == null || doc.getEmbeddingModel().isBlank()) {
				throw new BusinessException(
						"Document is missing embedding profile metadata: " + doc.getId(),
						HttpStatus.UNPROCESSABLE_ENTITY
				);
			}
			int dims = doc.getEmbeddingDimensions() != null && doc.getEmbeddingDimensions() > 0
					? doc.getEmbeddingDimensions()
					: appProperties.getEmbedding().getDimensions();
			EmbeddingProviderId providerId;
			try {
				providerId = EmbeddingProviderId.from(doc.getEmbeddingProvider());
			}
			catch (IllegalArgumentException ex) {
				throw new BusinessException(
						"Unsupported embedding provider on document: " + doc.getEmbeddingProvider(),
						HttpStatus.UNPROCESSABLE_ENTITY
				);
			}
			EmbeddingProfile profile = new EmbeddingProfile(providerId, doc.getEmbeddingModel(), dims);
			keys.add(profile.compatibilityKey());
			chosen = profile;
		}

		if (chosen == null) {
			return defaultProfile();
		}
		if (keys.size() > 1) {
			throw new BusinessException(
					"Selected documents use incompatible embedding profiles. Search one profile at a time.",
					HttpStatus.UNPROCESSABLE_ENTITY
			);
		}
		if (chosen.dimensions() != appProperties.getEmbedding().getDimensions()) {
			throw new BusinessException(
					"Document embedding dimensions (" + chosen.dimensions()
							+ ") do not match service configuration ("
							+ appProperties.getEmbedding().getDimensions() + ").",
					HttpStatus.UNPROCESSABLE_ENTITY
			);
		}
		return chosen;
	}

	public EmbeddingProfile defaultProfile() {
		EmbeddingProviderId providerId;
		try {
			providerId = EmbeddingProviderId.from(appProperties.getEmbedding().getDefaultProvider());
		}
		catch (IllegalArgumentException ex) {
			throw new BusinessException(
					"Invalid default embedding provider: " + appProperties.getEmbedding().getDefaultProvider(),
					HttpStatus.INTERNAL_SERVER_ERROR
			);
		}
		String model = switch (providerId) {
			case OLLAMA -> appProperties.getEmbedding().getOllama().getModel();
			case GEMINI -> appProperties.getEmbedding().getGemini().getModel();
		};
		return new EmbeddingProfile(providerId, model, appProperties.getEmbedding().getDimensions());
	}

	public void assertProviderModelMatch(EmbeddingProfile profile, QueryEmbeddingProvider provider) {
		if (profile.provider() != provider.id()) {
			throw new BusinessException(
					"Embedding provider mismatch: profile requires "
							+ profile.providerKey() + " but resolved " + provider.id().name().toLowerCase(Locale.ROOT),
					HttpStatus.UNPROCESSABLE_ENTITY
			);
		}
		if (!profile.model().equals(provider.model())) {
			throw new BusinessException(
					"Embedding model mismatch: profile requires '" + profile.model()
							+ "' but provider is configured for '" + provider.model() + "'.",
					HttpStatus.UNPROCESSABLE_ENTITY
			);
		}
	}
}
