package com.output_backend.ingestion;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentChunkReadRepository extends JpaRepository<DocumentChunkEntity, UUID> {

	List<DocumentChunkEntity> findByDocumentIdAndOwnerUserIdOrderByChunkIndexAsc(UUID documentId, UUID ownerUserId);

	long countByDocumentIdAndOwnerUserId(UUID documentId, UUID ownerUserId);

	@Query("""
			select c from DocumentChunkEntity c
			where c.ownerUserId = :ownerUserId
			  and c.documentId in :documentIds
			  and c.embeddingProvider = :embeddingProvider
			  and c.embeddingModel = :embeddingModel
			order by c.documentId, c.chunkIndex
			""")
	List<DocumentChunkEntity> findOwnedChunksForProfile(
			@Param("ownerUserId") UUID ownerUserId,
			@Param("documentIds") Collection<UUID> documentIds,
			@Param("embeddingProvider") String embeddingProvider,
			@Param("embeddingModel") String embeddingModel);

	@Query(value = """
			SELECT COUNT(*)
			FROM document_chunks
			WHERE owner_user_id = :ownerUserId
			  AND embedding IS NOT NULL
			""", nativeQuery = true)
	long countEmbeddedChunksForOwner(@Param("ownerUserId") UUID ownerUserId);
}
