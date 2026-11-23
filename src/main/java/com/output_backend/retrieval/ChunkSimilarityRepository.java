package com.output_backend.retrieval;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.output_backend.embedding.EmbeddingProfile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Parameterized pgvector cosine nearest-neighbor search over processing_backend chunks.
 */
@Repository
public class ChunkSimilarityRepository {

	private final JdbcTemplate jdbcTemplate;

	public ChunkSimilarityRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<RetrievedChunk> findSimilar(
			UUID ownerUserId,
			EmbeddingProfile profile,
			String queryVectorLiteral,
			Collection<UUID> documentIds,
			int topK
	) {
		if (topK <= 0) {
			return List.of();
		}

		StringBuilder sql = new StringBuilder("""
				SELECT dc.id,
				       dc.document_id,
				       d.original_filename,
				       dc.chunk_index,
				       dc.content,
				       dc.page_start,
				       dc.page_end,
				       dc.embedding_provider,
				       dc.embedding_model,
				       (dc.embedding <=> CAST(? AS vector)) AS distance
				FROM document_chunks dc
				JOIN documents d ON d.id = dc.document_id
				WHERE dc.owner_user_id = ?
				  AND d.owner_user_id = ?
				  AND d.status = 'COMPLETED'
				  AND dc.embedding IS NOT NULL
				  AND dc.embedding_provider = ?
				  AND dc.embedding_model = ?
				""");

		List<Object> args = new ArrayList<>();
		args.add(queryVectorLiteral);
		args.add(ownerUserId);
		args.add(ownerUserId);
		args.add(profile.providerKey());
		args.add(profile.model());

		if (documentIds != null && !documentIds.isEmpty()) {
			sql.append("  AND dc.document_id IN (");
			boolean first = true;
			for (UUID documentId : documentIds) {
				if (!first) {
					sql.append(',');
				}
				sql.append('?');
				args.add(documentId);
				first = false;
			}
			sql.append(")\n");
		}

		sql.append("""
				ORDER BY distance ASC
				LIMIT ?
				""");
		args.add(topK);

		return jdbcTemplate.query(
				sql.toString(),
				(rs, rowNum) -> {
					double distance = rs.getDouble("distance");
					double similarity = 1.0 - distance;
					return new RetrievedChunk(
							(UUID) rs.getObject("id"),
							(UUID) rs.getObject("document_id"),
							rs.getString("original_filename"),
							rs.getInt("chunk_index"),
							rs.getString("content"),
							(Integer) rs.getObject("page_start"),
							(Integer) rs.getObject("page_end"),
							rs.getString("embedding_provider"),
							rs.getString("embedding_model"),
							distance,
							similarity
					);
				},
				args.toArray()
		);
	}
}
