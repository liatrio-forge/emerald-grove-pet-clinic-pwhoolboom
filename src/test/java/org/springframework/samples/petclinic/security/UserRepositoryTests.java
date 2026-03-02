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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Integration tests for {@link UserRepository}.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class UserRepositoryTests {

	@Autowired
	private UserRepository userRepository;

	@Test
	void shouldPersistAndFindUserByEmail() {
		// Arrange
		User user = new User();
		user.setEmail("test@example.com");
		user.setPasswordHash(new BCryptPasswordEncoder().encode("password"));
		user.setRole(Role.ADMIN);

		// Act
		userRepository.save(user);
		Optional<User> found = userRepository.findByEmail("test@example.com");

		// Assert
		assertThat(found).isPresent();
		assertThat(found.get().getEmail()).isEqualTo("test@example.com");
		assertThat(found.get().getRole()).isEqualTo(Role.ADMIN);
		assertThat(found.get().getOwner()).isNull();
	}

	@Test
	void shouldReturnEmptyForUnknownEmail() {
		Optional<User> found = userRepository.findByEmail("nobody@example.com");
		assertThat(found).isEmpty();
	}

}
