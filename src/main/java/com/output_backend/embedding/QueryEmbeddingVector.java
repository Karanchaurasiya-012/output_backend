package com.output_backend.embedding;

import java.util.Arrays;

public record QueryEmbeddingVector(
		float[] values,
		String provider,
		String model,
		int dimensions
) {
	public QueryEmbeddingVector {
		if (values == null) {
			throw new IllegalArgumentException("embedding values required");
		}
		values = Arrays.copyOf(values, values.length);
		if (provider == null || provider.isBlank()) {
			throw new IllegalArgumentException("provider required");
		}
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("model required");
		}
		if (dimensions != values.length) {
			throw new IllegalArgumentException("dimensions must match values length");
		}
	}

	public float[] values() {
		return Arrays.copyOf(values, values.length);
	}

	public String toPgVectorLiteral() {
		StringBuilder sb = new StringBuilder(values.length * 8);
		sb.append('[');
		for (int i = 0; i < values.length; i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(values[i]);
		}
		sb.append(']');
		return sb.toString();
	}
}
