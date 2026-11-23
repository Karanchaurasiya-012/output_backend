package com.output_backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import com.output_backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class EmbeddingValidatorTest {

	private final EmbeddingValidator validator = new EmbeddingValidator();

	@Test
	void acceptsExactFiniteVector() {
		float[] values = new float[1024];
		Arrays.fill(values, 0.1f);
		assertThat(validator.requireValid(values, 1024)).hasSize(1024);
	}

	@Test
	void rejectsDimensionMismatchWithoutPadding() {
		float[] values = new float[10];
		assertThatThrownBy(() -> validator.requireValid(values, 1024))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("dimension mismatch");
	}

	@Test
	void rejectsNonFinite() {
		float[] values = new float[4];
		values[2] = Float.NaN;
		assertThatThrownBy(() -> validator.requireValid(values, 4))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("non-finite");
	}

	@Test
	void rejectsEmpty() {
		assertThatThrownBy(() -> validator.requireValid(new float[0], 1024))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void convertsDoubles() {
		float[] values = validator.fromDoubles(List.of(1.0, 2.0, 3.0), 3);
		assertThat(values).containsExactly(1.0f, 2.0f, 3.0f);
	}
}
