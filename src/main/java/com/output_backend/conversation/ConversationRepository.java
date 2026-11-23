package com.output_backend.conversation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	Optional<Conversation> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

	List<Conversation> findByOwnerUserIdOrderByUpdatedAtDesc(UUID ownerUserId);
}
