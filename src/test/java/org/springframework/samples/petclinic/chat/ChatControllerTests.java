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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.security.OwnerAuthenticationSuccessHandler;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.samples.petclinic.security.WebMvcTestSecurityConfig;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import reactor.core.publisher.Flux;

@WebMvcTest(ChatController.class)
@DisabledInNativeImage
@DisabledInAotMode
@WithMockUser
@Import(WebMvcTestSecurityConfig.class)
class ChatControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ChatService chatService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private OwnerAuthenticationSuccessHandler ownerAuthenticationSuccessHandler;

	@Test
	void validRequest_returns200AndSseContentType() throws Exception {
		given(chatService.chat(anyString(), anyString(), any())).willReturn(Flux.just("Hello", " world"));

		mockMvc
			.perform(post("/api/chat").with(csrf())
				.contentType(APPLICATION_JSON)
				.content("{\"message\":\"hi\",\"sessionId\":\"s1\"}"))
			.andExpect(status().isOk());
	}

	@Test
	void validRequest_responseIsTextEventStream() throws Exception {
		given(chatService.chat(anyString(), anyString(), any())).willReturn(Flux.just("Hello", " world"));

		mockMvc
			.perform(post("/api/chat").with(csrf())
				.contentType(APPLICATION_JSON)
				.content("{\"message\":\"hi\",\"sessionId\":\"s1\"}"))
			.andExpect(content().contentTypeCompatibleWith(TEXT_EVENT_STREAM));
	}

	@Test
	void blankMessage_returns400() throws Exception {
		mockMvc
			.perform(post("/api/chat").with(csrf())
				.contentType(APPLICATION_JSON)
				.content("{\"message\":\"\",\"sessionId\":\"s1\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void blankSessionId_returns400() throws Exception {
		mockMvc
			.perform(post("/api/chat").with(csrf())
				.contentType(APPLICATION_JSON)
				.content("{\"message\":\"hi\",\"sessionId\":\"\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	@WithAnonymousUser
	void unauthenticatedRequest_returns401() throws Exception {
		mockMvc
			.perform(post("/api/chat").with(csrf())
				.contentType(APPLICATION_JSON)
				.content("{\"message\":\"hi\",\"sessionId\":\"s1\"}"))
			.andExpect(status().isUnauthorized());
	}

}
