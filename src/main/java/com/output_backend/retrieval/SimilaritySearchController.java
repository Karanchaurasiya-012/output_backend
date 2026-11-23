package com.output_backend.retrieval;

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

@RestController
@RequestMapping("/api/v1/retrieval")
public class SimilaritySearchController {

	private final CurrentUser currentUser;
	private final SimilaritySearchService similaritySearchService;

	public SimilaritySearchController(CurrentUser currentUser, SimilaritySearchService similaritySearchService) {
		this.currentUser = currentUser;
		this.similaritySearchService = similaritySearchService;
	}

	@PostMapping("/search")
	public ResponseEntity<ApiResponse<Map<String, Object>>> search(@Valid @RequestBody SearchRequest request) {
		UUID owner = currentUser.requireUserId();
		SimilaritySearchService.SimilaritySearchResult result =
				similaritySearchService.search(owner, request.question(), request.documentIds());

		List<Map<String, Object>> chunks = result.chunks().stream()
				.map(chunk -> {
					Map<String, Object> row = new LinkedHashMap<>();
					row.put("chunkId", chunk.chunkId().toString());
					row.put("documentId", chunk.documentId().toString());
					row.put("documentName", chunk.documentName());
					row.put("chunkIndex", chunk.chunkIndex());
					row.put("pageStart", chunk.pageStart());
					row.put("pageEnd", chunk.pageEnd());
					row.put("similarity", chunk.similarity());
					row.put("cosineDistance", chunk.cosineDistance());
					row.put("excerpt", excerpt(chunk.content(), 400));
					return row;
				})
				.toList();

		Map<String, Object> data = new LinkedHashMap<>();
		data.put("embeddingProvider", result.profile().providerKey());
		data.put("embeddingModel", result.profile().model());
		data.put("topK", result.topK());
		data.put("similarityThreshold", result.similarityThreshold());
		data.put("candidatesConsidered", result.candidatesConsidered());
		data.put("hasRelevantContext", result.hasRelevantContext());
		data.put("chunks", chunks);

		String message = result.hasRelevantContext()
				? "Relevant chunks retrieved."
				: "No chunks met the similarity threshold.";
		return ResponseEntity.ok(ApiResponse.ok(message, data));
	}

	private static String excerpt(String content, int maxChars) {
		if (content == null) {
			return "";
		}
		String trimmed = content.strip();
		if (trimmed.length() <= maxChars) {
			return trimmed;
		}
		return trimmed.substring(0, maxChars) + "…";
	}

	public record SearchRequest(
			@NotBlank @Size(max = 8000) String question,
			List<UUID> documentIds
	) {
	}
}
