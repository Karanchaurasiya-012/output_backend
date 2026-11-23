package com.output_backend.cache;

import java.util.Map;
import java.util.UUID;

import com.output_backend.common.response.ApiResponse;
import com.output_backend.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cache")
public class CacheController {

	private final CurrentUser currentUser;
	private final RagCacheInvalidationService ragCacheInvalidationService;

	public CacheController(CurrentUser currentUser, RagCacheInvalidationService ragCacheInvalidationService) {
		this.currentUser = currentUser;
		this.ragCacheInvalidationService = ragCacheInvalidationService;
	}

	@PostMapping("/invalidate")
	public ResponseEntity<ApiResponse<Map<String, Object>>> invalidate() {
		UUID owner = currentUser.requireUserId();
		String version = ragCacheInvalidationService.invalidateForOwner(owner);
		return ResponseEntity.ok(ApiResponse.ok(
				"Owner retrieval/answer cache version bumped.",
				Map.of("ownerUserId", owner.toString(), "documentIndexVersion", version)
		));
	}
}
