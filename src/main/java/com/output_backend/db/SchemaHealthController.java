package com.output_backend.db;

import java.util.Map;

import com.output_backend.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SchemaHealthController {

	private final SchemaVerificationService schemaVerificationService;

	public SchemaHealthController(SchemaVerificationService schemaVerificationService) {
		this.schemaVerificationService = schemaVerificationService;
	}

	@GetMapping("/health/schema")
	public ResponseEntity<ApiResponse<Map<String, Object>>> schemaHealth() {
		Map<String, Object> report = schemaVerificationService.verify();
		boolean ok = Boolean.TRUE.equals(report.get("ok"));
		return ResponseEntity.ok(ApiResponse.ok(
				ok ? "Schema verification passed." : "Schema verification reported problems.",
				report));
	}
}
