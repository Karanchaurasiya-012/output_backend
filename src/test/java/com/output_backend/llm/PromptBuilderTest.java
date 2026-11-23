package com.output_backend.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.output_backend.context.BuiltContext;
import com.output_backend.conversation.ConversationMessage;
import com.output_backend.conversation.MessageRole;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

	private final PromptBuilder promptBuilder = new PromptBuilder();

	@Test
	void includesBoundedHistoryWithoutTreatingItAsDocumentContext() {
		BuiltContext context = new BuiltContext(
				"[S1] hello",
				List.of(),
				Map.of(),
				5,
				1800,
				1,
				0,
				0,
				0,
				false
		);
		ConversationMessage priorUser = new ConversationMessage();
		priorUser.setId(UUID.randomUUID());
		priorUser.setConversationId(UUID.randomUUID());
		priorUser.setOwnerUserId(UUID.randomUUID());
		priorUser.setRole(MessageRole.USER);
		priorUser.setContent("Earlier question");

		ConversationMessage priorAssistant = new ConversationMessage();
		priorAssistant.setId(UUID.randomUUID());
		priorAssistant.setConversationId(priorUser.getConversationId());
		priorAssistant.setOwnerUserId(priorUser.getOwnerUserId());
		priorAssistant.setRole(MessageRole.ASSISTANT);
		priorAssistant.setContent("Earlier answer");

		String prompt = promptBuilder.buildUserPrompt(
				"Follow-up?",
				context,
				List.of(priorUser, priorAssistant)
		);

		assertThat(prompt).contains("<<<CONTEXT>>>");
		assertThat(prompt).contains("<<<HISTORY>>>");
		assertThat(prompt).contains("USER: Earlier question");
		assertThat(prompt).contains("ASSISTANT: Earlier answer");
		assertThat(prompt).contains("not verified facts");
		assertThat(prompt).contains("Follow-up?");
		assertThat(PromptBuilder.SYSTEM_PROMPT).contains("not verified document evidence");
	}
}
