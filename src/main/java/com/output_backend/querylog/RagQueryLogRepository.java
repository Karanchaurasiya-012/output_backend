package com.output_backend.querylog;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RagQueryLogRepository extends JpaRepository<RagQueryLog, UUID> {
}
