package com.output_backend.context;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.output_backend.config.AppProperties;
import com.output_backend.retrieval.RetrievedChunk;
import org.springframework.stereotype.Service;

/**
 * Builds a bounded, deduplicated, citation-ready context block for LLM prompting.
 */
@Service
public class ContextBuilderService {

	private final TokenEstimator tokenEstimator;
	private final AppProperties appProperties;

	public ContextBuilderService(TokenEstimator tokenEstimator, AppProperties appProperties) {
		this.tokenEstimator = tokenEstimator;
		this.appProperties = appProperties;
	}

	public BuiltContext build(List<RetrievedChunk> retrievedChunks) {
		int budget = Math.max(1, appProperties.getRag().getContextTokenBudget());
		int inputCount = retrievedChunks == null ? 0 : retrievedChunks.size();

		if (retrievedChunks == null || retrievedChunks.isEmpty()) {
			return emptyResult(inputCount, budget, 0, 0, 0);
		}

		Set<UUID> seenChunkIds = new LinkedHashSet<>();
		Set<String> seenContentHashes = new LinkedHashSet<>();
		int duplicateChunkIds = 0;
		int duplicateContent = 0;

		List<RetrievedChunk> unique = new ArrayList<>();
		for (RetrievedChunk chunk : retrievedChunks) {
			if (chunk == null || chunk.chunkId() == null) {
				continue;
			}
			if (!seenChunkIds.add(chunk.chunkId())) {
				duplicateChunkIds++;
				continue;
			}
			String normalized = normalizeContent(chunk.content());
			if (normalized.isBlank()) {
				continue;
			}
			String hash = sha256(normalized);
			if (!seenContentHashes.add(hash)) {
				duplicateContent++;
				continue;
			}
			unique.add(chunk);
		}

		// Prefer higher similarity first when fitting the budget.
		unique.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));

		List<ContextSource> sources = new ArrayList<>();
		Map<UUID, ContextSource> byChunkId = new LinkedHashMap<>();
		StringBuilder rendered = new StringBuilder();
		int usedTokens = 0;
		int droppedForBudget = 0;
		int sourceIndex = 1;

		for (RetrievedChunk chunk : unique) {
			String header = formatHeader(sourceIndex, chunk);
			String body = chunk.content() == null ? "" : chunk.content().strip();
			String block = header + "\n" + body + "\n";
			int blockTokens = tokenEstimator.estimateTokens(block);

			if (usedTokens > 0 && usedTokens + blockTokens > budget) {
				droppedForBudget++;
				continue;
			}
			if (usedTokens == 0 && blockTokens > budget) {
				// Always keep at least a truncated first source if nothing fits.
				body = truncateToBudget(body, Math.max(32, budget - tokenEstimator.estimateTokens(header) - 8));
				block = header + "\n" + body + "\n";
				blockTokens = tokenEstimator.estimateTokens(block);
			}
			else if (usedTokens + blockTokens > budget) {
				droppedForBudget++;
				continue;
			}

			String sourceKey = "S" + sourceIndex;
			ContextSource source = new ContextSource(
					sourceKey,
					chunk.chunkId(),
					chunk.documentId(),
					chunk.documentName(),
					chunk.pageStart(),
					chunk.pageEnd(),
					chunk.chunkIndex(),
					chunk.similarity(),
					blockTokens
			);
			sources.add(source);
			byChunkId.put(chunk.chunkId(), source);

			if (!rendered.isEmpty()) {
				rendered.append('\n');
			}
			rendered.append(block);
			usedTokens += blockTokens;
			sourceIndex++;
		}

		boolean empty = sources.isEmpty();
		return new BuiltContext(
				empty ? "" : rendered.toString().strip(),
				List.copyOf(sources),
				Map.copyOf(byChunkId),
				usedTokens,
				budget,
				inputCount,
				duplicateChunkIds,
				duplicateContent,
				droppedForBudget,
				empty
		);
	}

	private static BuiltContext emptyResult(
			int inputCount,
			int budget,
			int dupIds,
			int dupContent,
			int dropped
	) {
		return new BuiltContext(
				"",
				List.of(),
				Map.of(),
				0,
				budget,
				inputCount,
				dupIds,
				dupContent,
				dropped,
				true
		);
	}

	private static String formatHeader(int index, RetrievedChunk chunk) {
		String doc = chunk.documentName() == null || chunk.documentName().isBlank()
				? "Unknown document"
				: chunk.documentName();
		String pages;
		if (chunk.pageStart() != null && chunk.pageEnd() != null) {
			pages = chunk.pageStart().equals(chunk.pageEnd())
					? "page " + chunk.pageStart()
					: "pages " + chunk.pageStart() + "-" + chunk.pageEnd();
		}
		else if (chunk.pageStart() != null) {
			pages = "page " + chunk.pageStart();
		}
		else {
			pages = "page unknown";
		}
		return "[S" + index + "] Document: " + doc + " | " + pages
				+ " | chunkId=" + chunk.chunkId()
				+ " | documentId=" + chunk.documentId();
	}

	private String truncateToBudget(String body, int tokenBudget) {
		if (tokenBudget <= 0 || body.isBlank()) {
			return "";
		}
		if (tokenEstimator.estimateTokens(body) <= tokenBudget) {
			return body;
		}
		// Shrink by words until under budget (still not char==token).
		String[] words = body.split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (String word : words) {
			String candidate = sb.isEmpty() ? word : sb + " " + word;
			if (tokenEstimator.estimateTokens(candidate + "…") > tokenBudget) {
				break;
			}
			sb.setLength(0);
			sb.append(candidate);
		}
		return sb.isEmpty() ? body.substring(0, Math.min(64, body.length())) + "…" : sb + "…";
	}

	static String normalizeContent(String content) {
		if (content == null) {
			return "";
		}
		return content
				.replace('\u00A0', ' ')
				.strip()
				.replaceAll("\\s+", " ")
				.toLowerCase(Locale.ROOT);
	}

	static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (Exception ex) {
			return Integer.toHexString(value.hashCode());
		}
	}
}
