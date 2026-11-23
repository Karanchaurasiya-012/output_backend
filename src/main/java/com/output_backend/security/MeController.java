package com.output_backend.security;

import java.util.Map;
import java.util.UUID;

import com.output_backend.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class MeController {

	private final CurrentUser currentUser;

	public MeController(CurrentUser currentUser) {
		this.currentUser = currentUser;
	}

	@GetMapping("/me")
	public ResponseEntity<ApiResponse<Map<String, Object>>> me() {
		UUID userId = currentUser.requireUserId();
		return ResponseEntity.ok(ApiResponse.ok(
				"Authenticated.",
				Map.of("userId", userId.toString())));
	}
}
