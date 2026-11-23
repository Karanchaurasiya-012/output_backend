package com.output_backend.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.context.BuiltContext;
import com.output_backend.context.ContextSource;
import org.junit.jupiter.api.Test;

class SourceCitationValidatorTest {

	private final SourceCitationValidator validator = new SourceCitationValidator();

	@Test
	void acceptsOnlyRetrievedSourceKeysAndBuildsBackendCitations() {
		UUID chunk1 = UUID.randomUUID();
		UUID chunk2 = UUID.randomUUID();
		ContextSource s1 = source("S1", chunk1, "Agreement.pdf", 4);
		ContextSource s2 = source("S2", chunk2, "Agreement.pdf", 5);
		BuiltContext context = new BuiltContext(
				"[S1] ...\n[S2] ...",
				List.of(s1, s2),
				Map.of(chunk1, s1, chunk2, s2),
				10,
				100,
				2,
				0,
				0,
				0,
				false
		);

		SourceCitationValidator.ValidatedAnswer validated = validator.validate(
				"The notice period is 30 days.\nSOURCES: S1, S9",
				context
		);

		assertThat(validated.answer()).contains("30 days");
		assertThat(validated.citations()).containsExactly(s1);
		assertThat(validated.rejectedSourceKeys()).containsExactly("S9");
		assertThat(validated.citations().get(0).documentName()).isEqualTo("Agreement.pdf");
		assertThat(validated.citations().get(0).pageStart()).isEqualTo(4);
	}

	@Test
	void noneSourcesYieldsEmptyCitations() {
		ContextSource s1 = source("S1", UUID.randomUUID(), "a.pdf", 1);
		BuiltContext context = new BuiltContext(
				"[S1]",
				List.of(s1),
				Map.of(s1.chunkId(), s1),
				5,
				100,
				1,
				0,
				0,
				0,
				false
		);

		SourceCitationValidator.ValidatedAnswer validated = validator.validate(
				"Not found.\nSOURCES: NONE",
				context
		);

		assertThat(validated.citations()).isEmpty();
		assertThat(validated.rejectedSourceKeys()).isEmpty();
	}

	private static ContextSource source(String key, UUID chunkId, String doc, int page) {
		return new ContextSource(key, chunkId, UUID.randomUUID(), doc, page, page, 0, 0.9, 12);
	}
}
