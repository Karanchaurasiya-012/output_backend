package com.output_backend.embedding;

import java.util.List;

import com.output_backend.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingValidator {

	public float[] requireValid(float[] values, int expectedDimensions) {
		if (values == null || values.length == 0) {
			throw new BusinessException("Embedding provider returned an empty vector.", HttpStatus.BAD_GATEWAY);
		}
		if (values.length != expectedDimensions) {
			throw new BusinessException(
					"Embedding dimension mismatch: expected " + expectedDimensions + " but got " + values.length + ".",
					HttpStatus.BAD_GATEWAY
			);
		}
		for (int i = 0; i < values.length; i++) {
			float v = values[i];
			if (!Float.isFinite(v)) {
				throw new BusinessException(
						"Embedding contains non-finite value at index " + i + ".",
						HttpStatus.BAD_GATEWAY
				);
			}
		}
		return values;
	}

	public float[] fromDoubles(List<Double> doubles, int expectedDimensions) {
		if (doubles == null) {
			throw new BusinessException("Embedding provider returned no vector.", HttpStatus.BAD_GATEWAY);
		}
		float[] values = new float[doubles.size()];
		for (int i = 0; i < doubles.size(); i++) {
			Double d = doubles.get(i);
			if (d == null) {
				throw new BusinessException("Embedding contains null at index " + i + ".", HttpStatus.BAD_GATEWAY);
			}
			values[i] = d.floatValue();
		}
		return requireValid(values, expectedDimensions);
	}
}
