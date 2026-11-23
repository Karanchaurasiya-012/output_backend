package com.output_backend.querylog;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "rag_query_logs")
public class RagQueryLog {

	@Id
	private UUID id;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Column(name = "conversation_id")
	private UUID conversationId;

	@Column(name = "query_hash", nullable = false, length = 64)
	private String queryHash;

	@Column(name = "embedding_provider", length = 50)
	private String embeddingProvider;

	@Column(name = "embedding_model", length = 150)
	private String embeddingModel;

	@Column(name = "llm_provider", length = 50)
	private String llmProvider;

	@Column(name = "llm_model", length = 150)
	private String llmModel;

	@Column(name = "retrieved_chunk_count")
	private Integer retrievedChunkCount;

	@Column(name = "cache_hit", nullable = false)
	private boolean cacheHit;

	@Column(name = "latency_ms")
	private Integer latencyMs;

	@Column(nullable = false, length = 30)
	private String status;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(UUID ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	public UUID getConversationId() {
		return conversationId;
	}

	public void setConversationId(UUID conversationId) {
		this.conversationId = conversationId;
	}

	public String getQueryHash() {
		return queryHash;
	}

	public void setQueryHash(String queryHash) {
		this.queryHash = queryHash;
	}

	public String getEmbeddingProvider() {
		return embeddingProvider;
	}

	public void setEmbeddingProvider(String embeddingProvider) {
		this.embeddingProvider = embeddingProvider;
	}

	public String getEmbeddingModel() {
		return embeddingModel;
	}

	public void setEmbeddingModel(String embeddingModel) {
		this.embeddingModel = embeddingModel;
	}

	public String getLlmProvider() {
		return llmProvider;
	}

	public void setLlmProvider(String llmProvider) {
		this.llmProvider = llmProvider;
	}

	public String getLlmModel() {
		return llmModel;
	}

	public void setLlmModel(String llmModel) {
		this.llmModel = llmModel;
	}

	public Integer getRetrievedChunkCount() {
		return retrievedChunkCount;
	}

	public void setRetrievedChunkCount(Integer retrievedChunkCount) {
		this.retrievedChunkCount = retrievedChunkCount;
	}

	public boolean isCacheHit() {
		return cacheHit;
	}

	public void setCacheHit(boolean cacheHit) {
		this.cacheHit = cacheHit;
	}

	public Integer getLatencyMs() {
		return latencyMs;
	}

	public void setLatencyMs(Integer latencyMs) {
		this.latencyMs = latencyMs;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
