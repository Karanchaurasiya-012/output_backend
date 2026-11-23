package com.output_backend.embedding;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
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

/**
 * Authenticated probe for Phase 4. Never returns raw embedding values.
 */
@RestController
@RequestMapping("/api/v1/embeddings")
public class QueryEmbeddingController {

	private final CurrentUser currentUser;
	private final QueryEmbeddingService queryEmbeddingService;

	public QueryEmbeddingController(CurrentUser currentUser, QueryEmbeddingService queryEmbeddingService) {
		this.currentUser = currentUser;
		this.queryEmbeddingService = queryEmbeddingService;
	}

	@PostMapping("/query")
	public ResponseEntity<ApiResponse<Map<String, Object>>> embedQuery(@Valid @RequestBody EmbedQueryRequest request) {
		UUID owner = currentUser.requireUserId();
		QueryEmbeddingService.QueryEmbeddingResult result = queryEmbeddingService.embedForDocuments(
				owner,
				request.question(),
				request.documentIds()
		);

		float[] values = result.vector().values();
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("provider", result.profile().providerKey());
		data.put("model", result.profile().model());
		data.put("dimensions", result.profile().dimensions());
		data.put("l2Norm", VectorMath.l2Norm(values));
		data.put("fingerprint", fingerprint(values));
		return ResponseEntity.ok(ApiResponse.ok("Query embedding generated.", data));
	}

	private static String fingerprint(float[] values) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			for (float v : values) {
				int bits = Float.floatToIntBits(v);
				digest.update((byte) (bits >>> 24));
				digest.update((byte) (bits >>> 16));
				digest.update((byte) (bits >>> 8));
				digest.update((byte) bits);
			}
			return HexFormat.of().formatHex(digest.digest());
		}
		catch (Exception ex) {
			return "unavailable";
		}
	}

	public record EmbedQueryRequest(
			@NotBlank @Size(max = 8000) String question,
			List<UUID> documentIds
	) {
	}
}
