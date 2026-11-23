package com.output_backend.conversation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

	private final ConversationRepository conversationRepository;
	private final ConversationMessageRepository conversationMessageRepository;
	private final AppProperties appProperties;

	public ConversationService(
			ConversationRepository conversationRepository,
			ConversationMessageRepository conversationMessageRepository,
			AppProperties appProperties) {
		this.conversationRepository = conversationRepository;
		this.conversationMessageRepository = conversationMessageRepository;
		this.appProperties = appProperties;
	}

	@Transactional
	public Conversation create(UUID ownerUserId, String title) {
		Conversation conversation = new Conversation();
		conversation.setOwnerUserId(ownerUserId);
		conversation.setTitle(normalizeTitle(title));
		return conversationRepository.save(conversation);
	}

	@Transactional(readOnly = true)
	public Conversation requireOwned(UUID conversationId, UUID ownerUserId) {
		return conversationRepository.findByIdAndOwnerUserId(conversationId, ownerUserId)
				.orElseThrow(() -> new BusinessException("Conversation not found.", HttpStatus.NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public Page<Conversation> list(UUID ownerUserId, int page, int size) {
		int safePage = Math.max(0, page);
		int safeSize = Math.min(100, Math.max(1, size));
		return conversationRepository.findByOwnerUserIdOrderByUpdatedAtDesc(
				ownerUserId, PageRequest.of(safePage, safeSize));
	}

	@Transactional(readOnly = true)
	public List<ConversationMessage> listMessages(UUID conversationId, UUID ownerUserId) {
		requireOwned(conversationId, ownerUserId);
		return conversationMessageRepository.findByConversationIdAndOwnerUserIdOrderByCreatedAtAsc(
				conversationId, ownerUserId);
	}

	@Transactional(readOnly = true)
	public List<ConversationMessage> loadRecentHistory(UUID conversationId, UUID ownerUserId) {
		requireOwned(conversationId, ownerUserId);
		int max = Math.max(0, appProperties.getRetention().getHistoryMaxMessages());
		if (max == 0) {
			return List.of();
		}
		List<ConversationMessage> newestFirst = conversationMessageRepository
				.findByConversationIdAndOwnerUserIdOrderByCreatedAtDesc(
						conversationId, ownerUserId, PageRequest.of(0, max));
		List<ConversationMessage> chronological = new ArrayList<>(newestFirst);
		Collections.reverse(chronological);
		return chronological;
	}

	@Transactional
	public Conversation resolveForChat(UUID ownerUserId, UUID conversationId, String question) {
		if (conversationId == null) {
			return create(ownerUserId, titleFromQuestion(question));
		}
		return requireOwned(conversationId, ownerUserId);
	}

	@Transactional
	public ConversationMessage appendMessage(
			Conversation conversation,
			UUID ownerUserId,
			MessageRole role,
			String content,
			String llmProvider,
			String llmModel
	) {
		if (!conversation.getOwnerUserId().equals(ownerUserId)) {
			throw new BusinessException("Conversation not found.", HttpStatus.NOT_FOUND);
		}
		ConversationMessage message = new ConversationMessage();
		message.setConversationId(conversation.getId());
		message.setOwnerUserId(ownerUserId);
		message.setRole(role);
		message.setContent(content == null ? "" : content);
		message.setLlmProvider(llmProvider);
		message.setLlmModel(llmModel);
		ConversationMessage saved = conversationMessageRepository.save(message);

		if ((conversation.getTitle() == null || conversation.getTitle().isBlank())
				&& role == MessageRole.USER) {
			conversation.setTitle(titleFromQuestion(content));
		}
		conversation.setUpdatedAt(Instant.now());
		conversationRepository.save(conversation);
		return saved;
	}

	@Transactional
	public void delete(UUID conversationId, UUID ownerUserId) {
		Conversation conversation = requireOwned(conversationId, ownerUserId);
		conversationMessageRepository.deleteByConversationIdAndOwnerUserId(conversationId, ownerUserId);
		conversationRepository.delete(conversation);
	}

	public Map<String, Object> toSummary(Conversation conversation) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("conversationId", conversation.getId().toString());
		row.put("title", conversation.getTitle());
		row.put("createdAt", conversation.getCreatedAt() == null ? null : conversation.getCreatedAt().toString());
		row.put("updatedAt", conversation.getUpdatedAt() == null ? null : conversation.getUpdatedAt().toString());
		return row;
	}

	public Map<String, Object> toMessageView(ConversationMessage message) {
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("id", message.getId().toString());
		row.put("conversationId", message.getConversationId().toString());
		row.put("role", message.getRole().name());
		row.put("content", message.getContent());
		row.put("llmProvider", message.getLlmProvider());
		row.put("llmModel", message.getLlmModel());
		row.put("createdAt", message.getCreatedAt() == null ? null : message.getCreatedAt().toString());
		return row;
	}

	private static String titleFromQuestion(String question) {
		if (question == null || question.isBlank()) {
			return "New conversation";
		}
		String trimmed = question.strip().replaceAll("\\s+", " ");
		return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80) + "…";
	}

	private static String normalizeTitle(String title) {
		if (title == null || title.isBlank()) {
			return null;
		}
		String trimmed = title.strip();
		return trimmed.length() <= 255 ? trimmed : trimmed.substring(0, 255);
	}
}
