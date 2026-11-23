package com.output_backend.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class RetrievedChunkScoringTest {

	@Test
	void similarityIsOneMinusCosineDistance() {
		RetrievedChunk chunk = new RetrievedChunk(
				UUID.randomUUID(),
				UUID.randomUUID(),
				"a.pdf",
				0,
				"text",
				1,
				1,
				"ollama",
				"bge-m3:latest",
				0.25,
				0.75
		);
		assertThat(chunk.similarity()).isCloseTo(1.0 - chunk.cosineDistance(), within(1e-9));
		assertThat(chunk.similarity()).isGreaterThanOrEqualTo(0.70);
	}
}
