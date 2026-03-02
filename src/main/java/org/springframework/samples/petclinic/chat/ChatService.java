/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

/**
 * Orchestrates the AI chat interaction: builds the prompt with the system message, user
 * message, and per-session memory advisor, then streams LLM tokens back as a
 * {@link Flux}.
 */
@Service
class ChatService {

	// @formatter:off
	private static final String SYSTEM_PROMPT = """
			You are the Emerald Grove Veterinary Clinic's virtual assistant.
			You help visitors with questions about our veterinarians, clinic services,
			pet care, and appointment scheduling.

			Use the provided tools to look up current information — do not guess or
			invent clinic data. If you need an owner's name to look up appointments,
			ask the user politely.

			Do not share personal contact details such as phone numbers or home
			addresses. Limit responses to topics relevant to the clinic.

			When a question is outside your scope, suggest the visitor call the clinic
			directly or use the Find Owners page to manage their account.
			""";
	// @formatter:on

	private final ChatClient chatClient;

	private final ChatMemory chatMemory;

	ChatService(ChatClient chatClient, ChatMemory chatMemory) {
		this.chatClient = chatClient;
		this.chatMemory = chatMemory;
	}

	Flux<String> chat(String sessionId, String message, String userContext) {
		String systemPrompt = (userContext != null && !userContext.isBlank()) ? userContext + "\n\n" + SYSTEM_PROMPT
				: SYSTEM_PROMPT;
		return chatClient.prompt()
			.system(systemPrompt)
			.user(message)
			.advisors(MessageChatMemoryAdvisor.builder(chatMemory).conversationId(sessionId).build())
			.stream()
			.content();
	}

}
