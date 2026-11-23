package com.output_backend.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.output_backend.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class VectorMathTest {

	@Test
	void l2NormalizeProducesUnitVector() {
		float[] normalized = VectorMath.l2Normalize(new float[]{3f, 4f});
		assertThat(normalized[0]).isCloseTo(0.6f, within(1e-6f));
		assertThat(normalized[1]).isCloseTo(0.8f, within(1e-6f));
		assertThat(VectorMath.l2Norm(normalized)).isCloseTo(1.0, within(1e-6));
	}

	@Test
	void rejectsZeroVector() {
		assertThatThrownBy(() -> VectorMath.l2Normalize(new float[]{0f, 0f}))
				.isInstanceOf(BusinessException.class);
	}
}
