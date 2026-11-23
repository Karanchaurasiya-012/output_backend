package com.output_backend.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class QueryHashTest {

	@Test
	void normalizesQuestionBeforeHash() {
		String a = QueryHash.ofQuestion("  Hello   World ");
		String b = QueryHash.ofQuestion("hello world");
		assertThat(a).isEqualTo(b);
		assertThat(a).hasSize(64);
	}

	@Test
	void documentScopeIsOrderIndependent() {
		UUID x = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
		UUID y = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
		assertThat(QueryHash.ofDocumentScope(List.of(x, y)))
				.isEqualTo(QueryHash.ofDocumentScope(List.of(y, x)));
		assertThat(QueryHash.ofDocumentScope(List.of())).isEqualTo("all");
	}
}
