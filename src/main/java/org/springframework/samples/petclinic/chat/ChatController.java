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

import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.security.User;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import reactor.core.publisher.Flux;

/**
 * REST controller that accepts user chat messages and streams LLM responses as
 * Server-Sent Events.
 */
@RestController
@RequestMapping("/api/chat")
class ChatController {

	private final ChatService chatService;

	private final UserRepository userRepository;

	ChatController(ChatService chatService, UserRepository userRepository) {
		this.chatService = chatService;
		this.userRepository = userRepository;
	}

	@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	Flux<ChatResponse> chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
		String userContext = buildUserContext(auth);
		return chatService.chat(request.sessionId(), request.message(), userContext).map(ChatResponse::new);
	}

	private String buildUserContext(Authentication auth) {
		if (auth == null) {
			return null;
		}
		return userRepository.findByEmail(auth.getName()).map(user -> {
			String displayName = user.getOwner() != null
					? user.getOwner().getFirstName() + " " + user.getOwner().getLastName() : user.getEmail();
			String role = user.getRole().name();
			if ("OWNER".equals(role)) {
				return "The current user is " + displayName
						+ " (role: OWNER). Only provide information relevant to this user's pets and visits.";
			}
			else {
				return "The current user is " + displayName
						+ " (role: ADMIN). This user has access to all clinic data.";
			}
		}).orElse(null);
	}

}
