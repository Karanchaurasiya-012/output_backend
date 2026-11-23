package com.output_backend.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DocumentAccessServiceTest {

	@Mock
	private DocumentReadRepository documentReadRepository;

	@Mock
	private DocumentChunkReadRepository documentChunkReadRepository;

	private DocumentAccessService service;

	@BeforeEach
	void setUp() {
		service = new DocumentAccessService(documentReadRepository, documentChunkReadRepository);
	}

	@Test
	void requireOwnedDocument_returnsWhenOwnerMatches() {
		UUID owner = UUID.randomUUID();
		UUID docId = UUID.randomUUID();
		DocumentEntity doc = new DocumentEntity();
		when(documentReadRepository.findByIdAndOwnerUserId(docId, owner)).thenReturn(Optional.of(doc));

		assertThat(service.requireOwnedDocument(docId, owner)).isSameAs(doc);
	}

	@Test
	void requireOwnedDocument_rejectsForeignOwner() {
		UUID owner = UUID.randomUUID();
		UUID other = UUID.randomUUID();
		UUID docId = UUID.randomUUID();
		when(documentReadRepository.findByIdAndOwnerUserId(docId, other)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.requireOwnedDocument(docId, other))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining("Document not found");
	}

	@Test
	void requireOwnedDocuments_rejectsPartialMatch() {
		UUID owner = UUID.randomUUID();
		UUID a = UUID.randomUUID();
		UUID b = UUID.randomUUID();
		DocumentEntity onlyA = new DocumentEntity();
		when(documentReadRepository.findOwnedByIds(owner, java.util.Set.of(a, b)))
				.thenReturn(List.of(onlyA));

		assertThatThrownBy(() -> service.requireOwnedDocuments(List.of(a, b), owner))
				.isInstanceOf(ResponseStatusException.class);
	}
}
