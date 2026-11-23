package com.output_backend.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HexFormat;
import java.util.Locale;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

public final class QueryHash {

	private QueryHash() {
	}

	public static String normalizeQuestion(String question) {
		if (question == null) {
			return "";
		}
		return question.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	public static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (Exception ex) {
			return Integer.toHexString(value == null ? 0 : value.hashCode());
		}
	}

	public static String ofQuestion(String question) {
		return sha256(normalizeQuestion(question));
	}

	public static String ofDocumentScope(Collection<UUID> documentIds) {
		if (documentIds == null || documentIds.isEmpty()) {
			return "all";
		}
		String joined = new TreeSet<>(documentIds).stream()
				.map(UUID::toString)
				.collect(Collectors.joining(","));
		return sha256(joined);
	}
}
