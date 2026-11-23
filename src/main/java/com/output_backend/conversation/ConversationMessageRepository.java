package com.output_backend.conversation;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

	List<ConversationMessage> findByConversationIdAndOwnerUserIdOrderByCreatedAtAsc(
			UUID conversationId,
			UUID ownerUserId);

	List<ConversationMessage> findByConversationIdAndOwnerUserIdOrderByCreatedAtDesc(
			UUID conversationId,
			UUID ownerUserId,
			Pageable pageable);

	long countByConversationIdAndOwnerUserId(UUID conversationId, UUID ownerUserId);

	void deleteByConversationIdAndOwnerUserId(UUID conversationId, UUID ownerUserId);
}
