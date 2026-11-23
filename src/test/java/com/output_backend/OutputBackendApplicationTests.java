package com.output_backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full context load needs live PostgreSQL + Redis.
 * Covered by later-phase integration / smoke tests.
 */
@SpringBootTest
@Disabled("Requires live PostgreSQL and Redis; enable in Phase 2+ integration tests")
class OutputBackendApplicationTests {

	@Test
	void contextLoads() {
	}
}
