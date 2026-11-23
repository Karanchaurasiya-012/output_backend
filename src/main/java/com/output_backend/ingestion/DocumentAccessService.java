package com.output_backend.ingestion;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Owner-scoped reads against processing_backend ingestion tables.
 */
@Service
public class DocumentAccessService {

	private final DocumentReadRepository documentReadRepository;
	private final DocumentChunkReadRepository documentChunkReadRepository;

	public DocumentAccessService(
			DocumentReadRepository documentReadRepository,
			DocumentChunkReadRepository documentChunkReadRepository) {
		this.documentReadRepository = documentReadRepository;
		this.documentChunkReadRepository = documentChunkReadRepository;
	}

	public List<DocumentEntity> listCompletedForOwner(UUID ownerUserId) {
		return documentReadRepository.findByOwnerUserIdAndStatusOrderByUpdatedAtDesc(
				ownerUserId, DocumentStatus.COMPLETED);
	}

	public DocumentEntity requireOwnedDocument(UUID documentId, UUID ownerUserId) {
		return documentReadRepository.findByIdAndOwnerUserId(documentId, ownerUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
	}

	public List<DocumentEntity> requireOwnedDocuments(Collection<UUID> documentIds, UUID ownerUserId) {
		if (documentIds == null || documentIds.isEmpty()) {
			return List.of();
		}
		Set<UUID> requested = new HashSet<>(documentIds);
		List<DocumentEntity> found = documentReadRepository.findOwnedByIds(ownerUserId, requested);
		if (found.size() != requested.size()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "One or more documents were not found");
		}
		return found;
	}

	public List<DocumentChunkEntity> listOwnedChunks(UUID documentId, UUID ownerUserId) {
		requireOwnedDocument(documentId, ownerUserId);
		return documentChunkReadRepository.findByDocumentIdAndOwnerUserIdOrderByChunkIndexAsc(
				documentId, ownerUserId);
	}

	public long countEmbeddedChunks(UUID ownerUserId) {
		return documentChunkReadRepository.countEmbeddedChunksForOwner(ownerUserId);
	}
}
