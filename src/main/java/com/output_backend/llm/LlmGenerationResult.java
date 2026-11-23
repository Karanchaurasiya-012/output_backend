package com.output_backend.llm;

public record LlmGenerationResult(
		String rawText,
		String provider,
		String model
) {
}
