package com.output_backend.context;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TokenEstimatorTest {

	private final TokenEstimator estimator = new TokenEstimator();

	@Test
	void emptyIsZero() {
		assertThat(estimator.estimateTokens("")).isZero();
		assertThat(estimator.estimateTokens(null)).isZero();
	}

	@Test
	void doesNotEqualCharacterCount() {
		String text = "The employee must provide thirty days notice.";
		int tokens = estimator.estimateTokens(text);
		assertThat(tokens).isPositive();
		assertThat(tokens).isNotEqualTo(text.length());
		assertThat(tokens).isLessThan(text.length());
	}
}
