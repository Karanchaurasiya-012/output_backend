package com.output_backend.db;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Startup checks: pgvector, ingestion schema, RAG-owned tables. Does not modify ingestion data.
 */
@Component
public class SchemaVerificationService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(SchemaVerificationService.class);

	private static final List<String> INGESTION_TABLES = List.of(
			"documents", "document_chunks", "pii_findings", "processing_jobs");

	private static final List<String> RAG_TABLES = List.of(
			"conversations", "conversation_messages", "rag_query_logs");

	private final JdbcTemplate jdbcTemplate;

	public SchemaVerificationService(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		Map<String, Object> report = verify();
		log.info("Phase-2 schema verification: {}", report);
		if (!Boolean.TRUE.equals(report.get("ok"))) {
			throw new IllegalStateException("Schema verification failed: " + report);
		}
	}

	public Map<String, Object> verify() {
		Map<String, Object> report = new LinkedHashMap<>();
		boolean ok = true;

		boolean vector = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
				"SELECT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector')",
				Boolean.class));
		report.put("pgvectorExtension", vector);
		ok &= vector;

		String embeddingType = jdbcTemplate.queryForObject("""
				SELECT format_type(a.atttypid, a.atttypmod)
				FROM pg_attribute a
				JOIN pg_class c ON c.oid = a.attrelid
				JOIN pg_namespace n ON n.oid = c.relnamespace
				WHERE n.nspname = 'public'
				  AND c.relname = 'document_chunks'
				  AND a.attname = 'embedding'
				  AND a.attnum > 0
				  AND NOT a.attisdropped
				""", String.class);
		report.put("documentChunksEmbeddingType", embeddingType);
		ok &= embeddingType != null && embeddingType.toLowerCase().startsWith("vector");

		boolean hnsw = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
				SELECT EXISTS (
				  SELECT 1 FROM pg_indexes
				  WHERE schemaname = 'public'
				    AND tablename = 'document_chunks'
				    AND indexname = 'idx_document_chunks_embedding_hnsw'
				)
				""", Boolean.class));
		report.put("hnswIndexPresent", hnsw);
		ok &= hnsw;

		for (String table : INGESTION_TABLES) {
			boolean present = tableExists(table);
			report.put("ingestion." + table, present);
			ok &= present;
		}

		for (String table : RAG_TABLES) {
			boolean present = tableExists(table);
			report.put("rag." + table, present);
			ok &= present;
		}

		boolean separateFlyway = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
				SELECT EXISTS (
				  SELECT 1 FROM information_schema.tables
				  WHERE table_schema = 'public' AND table_name = 'rag_flyway_schema_history'
				)
				""", Boolean.class));
		report.put("ragFlywayHistoryTable", separateFlyway);
		ok &= separateFlyway;

		report.put("ok", ok);
		return report;
	}

	private boolean tableExists(String table) {
		Boolean present = jdbcTemplate.queryForObject("""
				SELECT EXISTS (
				  SELECT 1 FROM information_schema.tables
				  WHERE table_schema = 'public' AND table_name = ?
				)
				""", Boolean.class, table);
		return Boolean.TRUE.equals(present);
	}
}
