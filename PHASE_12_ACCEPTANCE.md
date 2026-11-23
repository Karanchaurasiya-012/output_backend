# Phase 12 — Security and end-to-end testing

Date: 2026-11-23  
Branch: `phase-12`  
Maven evidence: `./mvnw test` → **Tests run: 67, Failures: 0, Errors: 0, Skipped: 1**, **BUILD SUCCESS**

## Acceptance matrix

| Requirement | Status | Evidence |
|---|---|---|
| JWT | PASS | `JwtSecurityWebMvcTest` (public health, 401 missing/invalid, authenticated `/api/v1/me`); `CookieBearerTokenResolverTest`; cookie name `__Host-access_token` |
| Ownership | PASS | `DocumentAccessServiceTest` foreign owner → 404; `ConversationServiceTest` / `ConversationOwnershipWebMvcTest` cross-user messages/delete → 404; `SimilaritySearchServiceTest.blocksSearchWhenDocumentNotOwned` |
| Query embedding | PASS | `EmbeddingProfileResolverTest`, `QueryEmbeddingServiceTest`, `EmbeddingValidatorTest` |
| Model compatibility | PASS | `EmbeddingProfileResolverTest.rejectsMixedProfiles`; retrieval cache filters foreign embedding profile |
| pgvector | PASS (unit) | `SimilaritySearchServiceTest` owner-scoped `findSimilar` + threshold filter; live DB search **BLOCKED** (services down) |
| Retrieval | PASS (unit) | threshold filter, ownership gate, cache hit path |
| No-context behavior | PASS | `AnswerGenerationServiceTest.skipsLlmWhenContextEmpty` |
| Qwen | BLOCKED | Ollama `127.0.0.1:11434` connection refused during Phase 12 probes |
| Gemini | BLOCKED | Live Gemini call not run (no running stack / avoid billing); unit `GeminiLlmProviderTest` PASS |
| Redis | BLOCKED (live) | `127.0.0.1:6379` connection refused; unit cache key isolation + docver bump PASS |
| Cache security | PASS | `CacheKeysIsolationTest` (owner isolation for embedding/retrieval/answer/docver); `DocumentIndexVersionServiceTest.bumpOnlyTouchesCallerOwnerKey` |
| Sources | PASS | `SourceCitationValidatorTest` maps only retrieved keys; rejects invented keys |
| Conversation | PASS | create/list/history ownership + bounded history unit tests |
| Frontend | NOT RUN | Authenticated browser chat against live APIs not executed (Auth/Processing/RAG not running) |
| Docker | NOT RUN | Phase 13 |
| Testing | PASS | This report + Maven surefire summary above |

## New / expanded automated gates (Phase 12)

- `CacheKeysIsolationTest`
- `DocumentIndexVersionServiceTest`
- `ConversationOwnershipWebMvcTest`
- `SimilaritySearchServiceTest` ownership + foreign-profile cache filter
- `Phase12SecurityGatesTest`

## Live probes (this session)

```
Redis: connection refused
Ollama: connection refused
RAG :8090: not running
Auth :8080: not running
Processing :8081: not running
```

Live end-to-end chat (JWT cookie → query → citations) remains **BLOCKED** until local stack is up. Unit/security gates above are green.

## How to re-run

```bash
cd output_backend
./mvnw -Dmaven.repo.local="$PWD/tmp/m2/repository" test
```
