# Phase 13 — Docker and deployment

Date: 2026-11-23  
Branch: `phase-13`

## Deliverables

| Artifact | Purpose |
|---|---|
| `Dockerfile` | Multi-stage Temurin 17 build → JRE runtime, port 8090, `curl` healthcheck |
| `.dockerignore` | Excludes `.env`, PEMs, `target/`, logs |
| `docker-compose.yml` | Private `redis` + `rag-query` (8090), host-gateway for Postgres/Ollama |

## Networking

- **Redis:** compose DNS `redis` (not published to host by default) — private cache network.
- **PostgreSQL:** existing shared instance via `host.docker.internal` (default `5432/postgres`). Prefer `rag_query` role. Override with `COMPOSE_DB_URL` (e.g. processing compose `5437/document_db`).
- **Ollama:** `COMPOSE_OLLAMA_BASE_URL` default `http://host.docker.internal:11434` — never use container `localhost` for host Ollama.
- **JWT:** `./keys/public.pem` → `/app/keys/public.pem` (read-only).

## Commands

```bash
cd output_backend
# .env must set DB_PASSWORD (and other secrets). Do not commit .env.
docker compose up --build -d
curl -fsS http://127.0.0.1:8090/api/v1/health
docker compose ps
docker compose down
```

## Verification evidence (this session)

| Check | Status | Evidence |
|---|---|---|
| `docker compose build` | PASS | Image `output_backend-rag-query:latest` built (Maven BUILD SUCCESS inside Dockerfile) |
| Redis private + healthcheck | PASS | `rag-query-redis` → `healthy`; `redis-cli ping` → `PONG`; no host port published |
| Compose healthchecks configured | PASS | Redis `redis-cli ping`; app `curl /api/v1/health` |
| Host gateway wiring | PASS | `extra_hosts: host.docker.internal:host-gateway`; compose forces non-localhost DB/Ollama URLs |
| App `/api/v1/health` via compose | BLOCKED | Host Postgres listens on `127.0.0.1:5432` only — Docker host-gateway cannot reach it. App exited with `Connection to host.docker.internal:5432 refused`. |
| Ollama from container | NOT RUN | Blocked behind Postgres startup failure |

### Unblock app health

1. Point Postgres at `0.0.0.0` / docker bridge (not `127.0.0.1` only), **or**
2. Run processing_backend compose Postgres and set  
   `COMPOSE_DB_URL=jdbc:postgresql://host.docker.internal:5437/document_db`, **or**
3. Run the JAR on the host against local PG (non-Docker) for local smoke.

Then: `docker compose up -d` and re-check `curl http://127.0.0.1:8090/api/v1/health`.
