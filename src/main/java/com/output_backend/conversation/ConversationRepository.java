package com.output_backend.conversation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

	Optional<Conversation> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);

	Page<Conversation> findByOwnerUserIdOrderByUpdatedAtDesc(UUID ownerUserId, Pageable pageable);
}
