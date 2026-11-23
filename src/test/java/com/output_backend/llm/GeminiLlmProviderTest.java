package com.output_backend.llm;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.output_backend.common.exception.BusinessException;
import com.output_backend.config.AppProperties;
import org.junit.jupiter.api.Test;

class GeminiLlmProviderTest {

	@Test
	void requiresApiKey() {
		AppProperties props = new AppProperties();
		props.getLlm().getGemini().setApiKey("");
		props.getLlm().getGemini().setModel("gemini-3.5-flash-lite");
		GeminiLlmProvider provider = new GeminiLlmProvider(props);

		assertThatThrownBy(() -> provider.generate("system", "user question"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("GEMINI_API_KEY");
	}
}
