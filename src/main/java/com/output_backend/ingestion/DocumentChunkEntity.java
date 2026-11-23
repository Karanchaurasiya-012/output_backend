package com.output_backend.ingestion;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Read-only chunk row. VECTOR embedding is accessed via JDBC in later retrieval phases —
 * intentionally unmapped here so Hibernate never writes embeddings.
 */
@Entity
@Immutable
@Table(name = "document_chunks")
public class DocumentChunkEntity {

	@Id
	private UUID id;

	@Column(name = "document_id", nullable = false)
	private UUID documentId;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(name = "chunk_index", nullable = false)
	private int chunkIndex;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "page_start")
	private Integer pageStart;

	@Column(name = "page_end")
	private Integer pageEnd;

	@Column(name = "token_count")
	private Integer tokenCount;

	@Column(name = "embedding_provider", length = 50)
	private String embeddingProvider;

	@Column(name = "embedding_model", length = 150)
	private String embeddingModel;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public UUID getId() {
		return id;
	}

	public UUID getDocumentId() {
		return documentId;
	}

	public UUID getOwnerUserId() {
		return ownerUserId;
	}

	public int getChunkIndex() {
		return chunkIndex;
	}

	public String getContent() {
		return content;
	}

	public Integer getPageStart() {
		return pageStart;
	}

	public Integer getPageEnd() {
		return pageEnd;
	}

	public Integer getTokenCount() {
		return tokenCount;
	}

	public String getEmbeddingProvider() {
		return embeddingProvider;
	}

	public String getEmbeddingModel() {
		return embeddingModel;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
