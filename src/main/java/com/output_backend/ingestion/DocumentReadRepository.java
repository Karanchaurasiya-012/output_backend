package com.output_backend.ingestion;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentReadRepository extends JpaRepository<DocumentEntity, UUID> {

	Optional<DocumentEntity> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

	List<DocumentEntity> findByOwnerUserIdAndStatusOrderByUpdatedAtDesc(UUID ownerUserId, DocumentStatus status);

	@Query("""
			select d from DocumentEntity d
			where d.ownerUserId = :ownerUserId
			  and d.id in :ids
			""")
	List<DocumentEntity> findOwnedByIds(
			@Param("ownerUserId") UUID ownerUserId,
			@Param("ids") Collection<UUID> ids);

	long countByOwnerUserIdAndStatus(UUID ownerUserId, DocumentStatus status);
}
