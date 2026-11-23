package com.output_backend.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class DocumentIndexVersionServiceTest {

	@Mock
	private StringRedisTemplate redis;

	@Mock
	private ValueOperations<String, String> values;

	private DocumentIndexVersionService service;

	@BeforeEach
	void setUp() {
		when(redis.opsForValue()).thenReturn(values);
		service = new DocumentIndexVersionService(redis);
	}

	@Test
	void bumpOnlyTouchesCallerOwnerKey() {
		UUID ownerA = UUID.randomUUID();
		UUID ownerB = UUID.randomUUID();
		String keyA = CacheKeys.documentIndexVersion(ownerA);
		when(values.increment(keyA)).thenReturn(4L);

		String version = service.bump(ownerA);

		assertThat(version).isEqualTo("4");
		verify(values).increment(keyA);
		verify(values, never()).increment(CacheKeys.documentIndexVersion(ownerB));
	}

	@Test
	void currentVersionDefaultsToZeroWhenMissing() {
		UUID owner = UUID.randomUUID();
		when(values.get(anyString())).thenReturn(null);

		assertThat(service.currentVersion(owner)).isEqualTo("0");
	}
}
