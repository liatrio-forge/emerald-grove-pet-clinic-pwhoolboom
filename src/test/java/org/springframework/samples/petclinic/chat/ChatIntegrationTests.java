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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.Container.ExecResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;

/**
 * End-to-end integration test that uses a real Ollama container (via TestContainers) to
 * verify that the {@code POST /api/chat} endpoint streams a non-empty response without
 * requiring an Anthropic API key.
 * <p>
 * The test is skipped gracefully when Docker is not available.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "spring.ai.anthropic.api-key=test-placeholder",
				"spring.main.allow-bean-definition-overriding=true" })
@Testcontainers(disabledWithoutDocker = true)
@DisabledInNativeImage
@DisabledInAotMode
class ChatIntegrationTests {

	@TestConfiguration
	static class TestChatConfig {

		@Bean
		ChatClient chatClient(ChatClient.Builder builder) {
			return builder.build();
		}

	}

	@Container
	static OllamaContainer ollama = new OllamaContainer(
			System.getProperty("ollama.docker.image", "ollama/ollama:0.4.7"));

	@LocalServerPort
	int port;

	@Autowired
	private RestTemplateBuilder builder;

	@BeforeAll
	static void pullModel() throws Exception {
		ExecResult result = ollama.execInContainer("ollama", "pull", "tinyllama");
		if (result.getExitCode() != 0) {
			throw new IllegalStateException(
					"Ollama model pull failed (exit " + result.getExitCode() + "): " + result.getStderr());
		}
	}

	@DynamicPropertySource
	static void ollamaProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.ai.model.chat", () -> "ollama");
		registry.add("spring.ai.ollama.base-url", ollama::getEndpoint);
		registry.add("spring.ai.ollama.chat.model", () -> "tinyllama");
	}

	@Test
	void chatEndpoint_streamsNonEmptyResponse() {
		RestTemplate template = builder.rootUri("http://localhost:" + port).build();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String body = "{\"message\":\"What pet types do you accept?\",\"sessionId\":\"it-test-1\"}";
		HttpEntity<String> request = new HttpEntity<>(body, headers);

		ResponseEntity<String> response = template.postForEntity("/api/chat", request, String.class);

		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isNotBlank();
	}

}
