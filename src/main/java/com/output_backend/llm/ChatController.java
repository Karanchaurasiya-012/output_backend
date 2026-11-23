package com.output_backend.llm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.response.ApiResponse;
import com.output_backend.conversation.Conversation;
import com.output_backend.conversation.ConversationMessage;
import com.output_backend.conversation.ConversationService;
import com.output_backend.conversation.MessageRole;
import com.output_backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

	private final CurrentUser currentUser;
	private final AnswerGenerationService answerGenerationService;
	private final ConversationService conversationService;

	public ChatController(
			CurrentUser currentUser,
			AnswerGenerationService answerGenerationService,
			ConversationService conversationService
	) {
		this.currentUser = currentUser;
		this.answerGenerationService = answerGenerationService;
		this.conversationService = conversationService;
	}

	@PostMapping("/query")
	public ResponseEntity<ApiResponse<Map<String, Object>>> query(@Valid @RequestBody ChatQueryRequest request) {
		UUID owner = currentUser.requireUserId();
		Conversation conversation = conversationService.resolveForChat(
				owner, request.conversationId(), request.question());
		List<ConversationMessage> history = conversationService.loadRecentHistory(conversation.getId(), owner);

		AnswerGenerationService.AnswerResult result = answerGenerationService.answer(
				owner,
				request.question(),
				request.documentIds(),
				request.llmProvider(),
				history
		);

		conversationService.appendMessage(
				conversation, owner, MessageRole.USER, request.question(), null, null);
		conversationService.appendMessage(
				conversation,
				owner,
				MessageRole.ASSISTANT,
				result.answer(),
				result.llmProvider(),
				result.llmModel()
		);

		Map<String, Object> data = new LinkedHashMap<>(result.toSafeView());
		data.put("conversationId", conversation.getId().toString());

		String message = result.noContext()
				? "No relevant document context was found."
				: "Answer generated successfully.";
		return ResponseEntity.ok(ApiResponse.ok(message, data));
	}

	public record ChatQueryRequest(
			@NotBlank @Size(max = 8000) String question,
			List<UUID> documentIds,
			UUID conversationId,
			String llmProvider
	) {
	}
}
