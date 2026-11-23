-- RAG Query Service owned tables (same PostgreSQL database as processing_backend).
-- Does NOT alter documents / document_chunks / pii_findings / processing_jobs.
-- Flyway history for this service uses table rag_flyway_schema_history (see application.yml).

CREATE TABLE conversations (
    id              UUID PRIMARY KEY,
    owner_user_id   UUID NOT NULL,
    title           VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_conversations_owner_updated
    ON conversations (owner_user_id, updated_at DESC);

CREATE TABLE conversation_messages (
    id                UUID PRIMARY KEY,
    conversation_id   UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    owner_user_id     UUID NOT NULL,
    role              VARCHAR(20) NOT NULL,
    content           TEXT NOT NULL,
    llm_provider      VARCHAR(50),
    llm_model         VARCHAR(150),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_conversation_messages_role
        CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_conversation_messages_conversation_created
    ON conversation_messages (conversation_id, created_at ASC);

CREATE INDEX idx_conversation_messages_owner
    ON conversation_messages (owner_user_id);

CREATE TABLE rag_query_logs (
    id                      UUID PRIMARY KEY,
    owner_user_id           UUID NOT NULL,
    conversation_id         UUID REFERENCES conversations (id) ON DELETE SET NULL,
    query_hash              VARCHAR(64) NOT NULL,
    embedding_provider      VARCHAR(50),
    embedding_model         VARCHAR(150),
    llm_provider            VARCHAR(50),
    llm_model               VARCHAR(150),
    retrieved_chunk_count   INTEGER,
    cache_hit               BOOLEAN NOT NULL DEFAULT FALSE,
    latency_ms              INTEGER,
    status                  VARCHAR(30) NOT NULL,
    error_code              VARCHAR(100),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rag_query_logs_owner_created
    ON rag_query_logs (owner_user_id, created_at DESC);

CREATE INDEX idx_rag_query_logs_conversation
    ON rag_query_logs (conversation_id);

COMMENT ON TABLE rag_query_logs IS
    'Operational metrics only. Do not store raw user queries or document text.';

COMMENT ON TABLE conversation_messages IS
    'Retention: delete with parent conversation; default policy 90 days (app-enforced).';
