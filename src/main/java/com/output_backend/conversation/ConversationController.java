package com.output_backend.conversation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.response.ApiResponse;
import com.output_backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

	private final CurrentUser currentUser;
	private final ConversationService conversationService;

	public ConversationController(CurrentUser currentUser, ConversationService conversationService) {
		this.currentUser = currentUser;
		this.conversationService = conversationService;
	}

	@PostMapping
	public ResponseEntity<ApiResponse<Map<String, Object>>> create(
			@Valid @RequestBody(required = false) CreateConversationRequest request
	) {
		UUID owner = currentUser.requireUserId();
		String title = request == null ? null : request.title();
		Conversation conversation = conversationService.create(owner, title);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("conversationId", conversation.getId().toString());
		data.put("title", conversation.getTitle());
		data.put("createdAt", conversation.getCreatedAt() == null ? null : conversation.getCreatedAt().toString());
		data.put("updatedAt", conversation.getUpdatedAt() == null ? null : conversation.getUpdatedAt().toString());
		return ResponseEntity.ok(ApiResponse.ok("Conversation created.", data));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<Map<String, Object>>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		UUID owner = currentUser.requireUserId();
		Page<Conversation> result = conversationService.list(owner, page, size);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("page", result.getNumber());
		data.put("size", result.getSize());
		data.put("totalElements", result.getTotalElements());
		data.put("totalPages", result.getTotalPages());
		data.put("items", result.getContent().stream().map(conversationService::toSummary).toList());
		return ResponseEntity.ok(ApiResponse.ok("Conversations listed.", data));
	}

	@GetMapping("/{conversationId}/messages")
	public ResponseEntity<ApiResponse<Map<String, Object>>> messages(@PathVariable UUID conversationId) {
		UUID owner = currentUser.requireUserId();
		List<ConversationMessage> messages = conversationService.listMessages(conversationId, owner);
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("conversationId", conversationId.toString());
		data.put("messages", messages.stream().map(conversationService::toMessageView).toList());
		return ResponseEntity.ok(ApiResponse.ok("Conversation messages loaded.", data));
	}

	@DeleteMapping("/{conversationId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable UUID conversationId) {
		UUID owner = currentUser.requireUserId();
		conversationService.delete(conversationId, owner);
		return ResponseEntity.ok(ApiResponse.ok(
				"Conversation deleted.",
				Map.of("conversationId", conversationId.toString())
		));
	}

	public record CreateConversationRequest(@Size(max = 255) String title) {
	}
}
