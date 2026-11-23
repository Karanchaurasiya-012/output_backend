package com.output_backend.embedding;

import com.output_backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

final class VectorMath {

	private VectorMath() {
	}

	static float[] l2Normalize(float[] values) {
		double sumSquares = 0.0;
		for (float v : values) {
			sumSquares += (double) v * (double) v;
		}
		double norm = Math.sqrt(sumSquares);
		if (!(norm > 0.0) || !Double.isFinite(norm)) {
			throw new BusinessException("Cannot L2-normalize a zero or non-finite embedding.", HttpStatus.BAD_GATEWAY);
		}
		float[] out = new float[values.length];
		for (int i = 0; i < values.length; i++) {
			out[i] = (float) (values[i] / norm);
		}
		return out;
	}

	static double l2Norm(float[] values) {
		double sumSquares = 0.0;
		for (float v : values) {
			sumSquares += (double) v * (double) v;
		}
		return Math.sqrt(sumSquares);
	}
}
