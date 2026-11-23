package com.output_backend.context;

import org.springframework.stereotype.Component;

/**
 * Approximate token counter for context budgeting.
 * Does <strong>not</strong> treat character length as token count.
 * Heuristic: whitespace/punctuation split with subword overhead factor.
 */
@Component
public class TokenEstimator {

	private static final double SUBWORD_OVERHEAD = 1.35;

	public int estimateTokens(String text) {
		if (text == null || text.isBlank()) {
			return 0;
		}
		String[] parts = text.trim().split("[\\s\\p{Punct}]+");
		int pieces = 0;
		for (String part : parts) {
			if (!part.isBlank()) {
				pieces++;
			}
		}
		if (pieces == 0) {
			return 1;
		}
		return Math.max(1, (int) Math.ceil(pieces * SUBWORD_OVERHEAD));
	}
}
