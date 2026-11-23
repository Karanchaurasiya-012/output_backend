package com.output_backend.llm;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.common.response.ApiResponse;
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

	public ChatController(CurrentUser currentUser, AnswerGenerationService answerGenerationService) {
		this.currentUser = currentUser;
		this.answerGenerationService = answerGenerationService;
	}

	@PostMapping("/query")
	public ResponseEntity<ApiResponse<Map<String, Object>>> query(@Valid @RequestBody ChatQueryRequest request) {
		UUID owner = currentUser.requireUserId();
		AnswerGenerationService.AnswerResult result = answerGenerationService.answer(
				owner,
				request.question(),
				request.documentIds(),
				request.llmProvider()
		);

		String message = result.noContext()
				? "No relevant document context was found."
				: "Answer generated successfully.";
		return ResponseEntity.ok(ApiResponse.ok(message, result.toSafeView()));
	}

	public record ChatQueryRequest(
			@NotBlank @Size(max = 8000) String question,
			List<UUID> documentIds,
			String llmProvider
	) {
	}
}
