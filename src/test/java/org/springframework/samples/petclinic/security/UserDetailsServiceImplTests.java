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

package org.springframework.samples.petclinic.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTests {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserDetailsServiceImpl userDetailsService;

	@Test
	void loadUserByUsername_returnsUserDetailsForValidEmail() {
		User user = new User();
		user.setEmail("george.franklin@petclinic.com");
		user.setPasswordHash("$2a$10$hashed");
		user.setRole(Role.OWNER);

		when(userRepository.findByEmail("george.franklin@petclinic.com")).thenReturn(Optional.of(user));

		UserDetails details = userDetailsService.loadUserByUsername("george.franklin@petclinic.com");

		assertThat(details.getUsername()).isEqualTo("george.franklin@petclinic.com");
		assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
	}

	@Test
	void loadUserByUsername_throwsUsernameNotFoundForUnknownEmail() {
		when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nobody@example.com"))
			.isInstanceOf(UsernameNotFoundException.class);
	}

}
