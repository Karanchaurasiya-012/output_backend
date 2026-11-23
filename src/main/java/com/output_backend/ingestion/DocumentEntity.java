package com.output_backend.ingestion;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Read-only view of processing_backend {@code documents}. Never persist from RAG.
 */
@Entity
@Immutable
@Table(name = "documents")
public class DocumentEntity {

	@Id
	private UUID id;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(name = "original_filename", nullable = false, length = 255)
	private String originalFilename;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(name = "file_size", nullable = false)
	private long fileSize;

	@Column(name = "checksum_sha256", nullable = false, length = 64)
	private String checksumSha256;

	@Column(name = "s3_bucket", length = 255)
	private String s3Bucket;

	@Column(name = "s3_key", length = 1024)
	private String s3Key;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private DocumentStatus status;

	@Column(name = "pii_detected", nullable = false)
	private boolean piiDetected;

	@Column(name = "pii_decision", length = 20)
	private String piiDecision;

	@Column(name = "masking_applied", nullable = false)
	private boolean maskingApplied;

	@Column(name = "page_count")
	private Integer pageCount;

	@Column(name = "chunk_count")
	private Integer chunkCount;

	@Column(name = "embedding_provider", length = 50)
	private String embeddingProvider;

	@Column(name = "embedding_model", length = 150)
	private String embeddingModel;

	@Column(name = "embedding_dimensions")
	private Integer embeddingDimensions;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public UUID getId() {
		return id;
	}

	public UUID getOwnerUserId() {
		return ownerUserId;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public String getContentType() {
		return contentType;
	}

	public long getFileSize() {
		return fileSize;
	}

	public String getChecksumSha256() {
		return checksumSha256;
	}

	public String getS3Bucket() {
		return s3Bucket;
	}

	public String getS3Key() {
		return s3Key;
	}

	public DocumentStatus getStatus() {
		return status;
	}

	public boolean isPiiDetected() {
		return piiDetected;
	}

	public String getPiiDecision() {
		return piiDecision;
	}

	public boolean isMaskingApplied() {
		return maskingApplied;
	}

	public Integer getPageCount() {
		return pageCount;
	}

	public Integer getChunkCount() {
		return chunkCount;
	}

	public String getEmbeddingProvider() {
		return embeddingProvider;
	}

	public String getEmbeddingModel() {
		return embeddingModel;
	}

	public Integer getEmbeddingDimensions() {
		return embeddingDimensions;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
