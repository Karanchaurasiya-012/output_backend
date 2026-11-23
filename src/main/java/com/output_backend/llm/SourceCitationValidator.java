package com.output_backend.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.output_backend.context.BuiltContext;
import com.output_backend.context.ContextSource;
import org.springframework.stereotype.Component;

/**
 * Parses model-claimed source keys and replaces them with backend citation metadata only.
 */
@Component
public class SourceCitationValidator {

	private static final Pattern SOURCES_LINE = Pattern.compile(
			"(?im)^\\s*SOURCES:\\s*(.+?)\\s*$");
	private static final Pattern SOURCE_KEY = Pattern.compile("\\bS(\\d+)\\b", Pattern.CASE_INSENSITIVE);

	public ValidatedAnswer validate(String rawLlmText, BuiltContext context) {
		String raw = rawLlmText == null ? "" : rawLlmText.strip();
		String answerText = stripSourcesLine(raw);

		Map<String, ContextSource> byKey = new LinkedHashMap<>();
		for (ContextSource source : context.sources()) {
			byKey.put(source.sourceKey().toUpperCase(Locale.ROOT), source);
		}

		Set<String> claimed = extractClaimedKeys(raw);
		List<ContextSource> accepted = new ArrayList<>();
		List<String> rejectedKeys = new ArrayList<>();

		for (String key : claimed) {
			ContextSource source = byKey.get(key.toUpperCase(Locale.ROOT));
			if (source == null) {
				rejectedKeys.add(key);
			}
			else if (accepted.stream().noneMatch(s -> s.sourceKey().equals(source.sourceKey()))) {
				accepted.add(source);
			}
		}

		// If the model forgot SOURCES but mentioned [S#] inline, already covered by extractClaimedKeys.
		// If still empty but context exists, do not invent citations — return answer without sources.
		return new ValidatedAnswer(answerText.strip(), List.copyOf(accepted), List.copyOf(rejectedKeys));
	}

	private static String stripSourcesLine(String raw) {
		Matcher matcher = SOURCES_LINE.matcher(raw);
		if (matcher.find()) {
			return matcher.replaceFirst("").strip();
		}
		return raw;
	}

	private static Set<String> extractClaimedKeys(String raw) {
		Set<String> keys = new LinkedHashSet<>();
		Matcher sourcesLine = SOURCES_LINE.matcher(raw);
		String searchSpace = raw;
		if (sourcesLine.find()) {
			String list = sourcesLine.group(1).trim();
			if (!list.equalsIgnoreCase("NONE")) {
				searchSpace = list + "\n" + raw;
			}
			else {
				return keys;
			}
		}
		Matcher keyMatcher = SOURCE_KEY.matcher(searchSpace);
		while (keyMatcher.find()) {
			keys.add("S" + keyMatcher.group(1));
		}
		return keys;
	}

	public record ValidatedAnswer(
			String answer,
			List<ContextSource> citations,
			List<String> rejectedSourceKeys
	) {
	}
}
