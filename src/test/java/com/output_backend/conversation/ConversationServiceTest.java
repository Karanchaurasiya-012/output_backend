package com.output_backend.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

	@Mock
	private ConversationRepository conversationRepository;

	@Mock
	private ConversationMessageRepository conversationMessageRepository;

	private ConversationService service;

	@BeforeEach
	void setUp() {
		AppProperties props = new AppProperties();
		props.getRetention().setHistoryMaxMessages(2);
		service = new ConversationService(conversationRepository, conversationMessageRepository, props);
	}

	@Test
	void requireOwnedRejectsForeignConversation() {
		UUID owner = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();
		when(conversationRepository.findByIdAndOwnerUserId(conversationId, owner)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.requireOwned(conversationId, owner))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Conversation not found");
	}

	@Test
	void loadRecentHistoryReturnsChronologicalBoundedWindow() {
		UUID owner = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();
		Conversation conversation = new Conversation();
		conversation.setId(conversationId);
		conversation.setOwnerUserId(owner);
		when(conversationRepository.findByIdAndOwnerUserId(conversationId, owner))
				.thenReturn(Optional.of(conversation));

		ConversationMessage newest = message(conversationId, owner, MessageRole.ASSISTANT, "a2");
		ConversationMessage older = message(conversationId, owner, MessageRole.USER, "q2");
		when(conversationMessageRepository.findByConversationIdAndOwnerUserIdOrderByCreatedAtDesc(
				eq(conversationId), eq(owner), eq(PageRequest.of(0, 2))))
				.thenReturn(List.of(newest, older));

		List<ConversationMessage> history = service.loadRecentHistory(conversationId, owner);

		assertThat(history).containsExactly(older, newest);
	}

	@Test
	void resolveForChatCreatesWhenConversationIdMissing() {
		UUID owner = UUID.randomUUID();
		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
			Conversation conversation = invocation.getArgument(0);
			if (conversation.getId() == null) {
				conversation.setId(UUID.randomUUID());
			}
			return conversation;
		});

		Conversation created = service.resolveForChat(owner, null, "What is the notice period?");

		assertThat(created.getOwnerUserId()).isEqualTo(owner);
		assertThat(created.getTitle()).startsWith("What is the notice period");
		verify(conversationRepository).save(any(Conversation.class));
	}

	@Test
	void deleteRequiresOwnership() {
		UUID owner = UUID.randomUUID();
		UUID conversationId = UUID.randomUUID();
		when(conversationRepository.findByIdAndOwnerUserId(conversationId, owner)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.delete(conversationId, owner))
				.isInstanceOf(BusinessException.class);
		verify(conversationMessageRepository, never())
				.deleteByConversationIdAndOwnerUserId(any(), any());
	}

	@Test
	void appendMessagePersistsRoleAndTouchesConversation() {
		UUID owner = UUID.randomUUID();
		Conversation conversation = new Conversation();
		conversation.setId(UUID.randomUUID());
		conversation.setOwnerUserId(owner);
		conversation.setTitle(null);

		when(conversationMessageRepository.save(any(ConversationMessage.class))).thenAnswer(invocation -> {
			ConversationMessage message = invocation.getArgument(0);
			if (message.getId() == null) {
				message.setId(UUID.randomUUID());
			}
			return message;
		});
		when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.appendMessage(conversation, owner, MessageRole.USER, "Hello world", null, null);

		ArgumentCaptor<ConversationMessage> messageCaptor = ArgumentCaptor.forClass(ConversationMessage.class);
		verify(conversationMessageRepository).save(messageCaptor.capture());
		assertThat(messageCaptor.getValue().getRole()).isEqualTo(MessageRole.USER);
		assertThat(messageCaptor.getValue().getContent()).isEqualTo("Hello world");
		assertThat(conversation.getTitle()).isEqualTo("Hello world");
		assertThat(conversation.getUpdatedAt()).isNotNull();
	}

	private static ConversationMessage message(
			UUID conversationId,
			UUID owner,
			MessageRole role,
			String content
	) {
		ConversationMessage message = new ConversationMessage();
		message.setId(UUID.randomUUID());
		message.setConversationId(conversationId);
		message.setOwnerUserId(owner);
		message.setRole(role);
		message.setContent(content);
		return message;
	}
}
