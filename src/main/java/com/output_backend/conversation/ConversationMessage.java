package com.output_backend.conversation;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_messages")
public class ConversationMessage {

	@Id
	private UUID id;

	@Column(name = "conversation_id", nullable = false)
	private UUID conversationId;

	@Column(name = "owner_user_id", nullable = false)
	private UUID ownerUserId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MessageRole role;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(name = "llm_provider", length = 50)
	private String llmProvider;

	@Column(name = "llm_model", length = 150)
	private String llmModel;

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

	public UUID getConversationId() {
		return conversationId;
	}

	public void setConversationId(UUID conversationId) {
		this.conversationId = conversationId;
	}

	public UUID getOwnerUserId() {
		return ownerUserId;
	}

	public void setOwnerUserId(UUID ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	public MessageRole getRole() {
		return role;
	}

	public void setRole(MessageRole role) {
		this.role = role;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
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

	public Instant getCreatedAt() {
		return createdAt;
	}
}
