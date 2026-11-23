package com.output_backend.llm;

/**
 * Chat/completion provider. Implementations must not silently fall back to another provider.
 */
public interface LlmProvider {

	LlmProviderId id();

	String model();

	LlmGenerationResult generate(String systemPrompt, String userPrompt);
}
