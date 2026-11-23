package com.output_backend.context;

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
@RequestMapping("/api/v1/context")
public class ContextController {

	private final CurrentUser currentUser;
	private final ContextPreparationService contextPreparationService;

	public ContextController(CurrentUser currentUser, ContextPreparationService contextPreparationService) {
		this.currentUser = currentUser;
		this.contextPreparationService = contextPreparationService;
	}

	@PostMapping("/prepare")
	public ResponseEntity<ApiResponse<Map<String, Object>>> prepare(@Valid @RequestBody PrepareRequest request) {
		UUID owner = currentUser.requireUserId();
		ContextPreparationService.ContextPreparationResult result =
				contextPreparationService.prepare(owner, request.question(), request.documentIds());

		String message = result.context().empty()
				? "No usable context after retrieval/dedupe/budget."
				: "Context prepared.";
		return ResponseEntity.ok(ApiResponse.ok(message, result.toSafeView()));
	}

	public record PrepareRequest(
			@NotBlank @Size(max = 8000) String question,
			List<UUID> documentIds
	) {
	}
}
