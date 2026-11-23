-- Ops script (run manually as a PostgreSQL superuser / table owner).
-- NOT executed by Flyway. Creates a dedicated app role that:
--   - SELECT-only on ingestion tables owned by processing_backend
--   - READ/WRITE on RAG-owned tables
--
-- Replace CHANGE_ME before running. Do not commit real passwords.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'rag_query') THEN
        CREATE ROLE rag_query LOGIN PASSWORD 'CHANGE_ME';
    END IF;
END
$$;

GRANT CONNECT ON DATABASE postgres TO rag_query;
GRANT USAGE ON SCHEMA public TO rag_query;

-- Ingestion tables: read-only
GRANT SELECT ON TABLE documents TO rag_query;
GRANT SELECT ON TABLE document_chunks TO rag_query;
GRANT SELECT ON TABLE pii_findings TO rag_query;
GRANT SELECT ON TABLE processing_jobs TO rag_query;

-- RAG-owned tables: DML (create tables first via Flyway as owner)
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE conversations TO rag_query;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE conversation_messages TO rag_query;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE rag_query_logs TO rag_query;

-- Explicit denials are not required; never GRANT INSERT/UPDATE/DELETE on ingestion tables.

-- Optional: revoke public write defaults if your hardening policy requires it.
-- REVOKE ALL ON TABLE documents FROM PUBLIC;
