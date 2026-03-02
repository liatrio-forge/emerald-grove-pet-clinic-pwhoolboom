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

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegistrationService {

	private final UserRepository userRepository;

	private final OwnerRepository ownerRepository;

	private final PasswordEncoder passwordEncoder;

	RegistrationService(UserRepository userRepository, OwnerRepository ownerRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.ownerRepository = ownerRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	User register(RegistrationForm form) {
		if (userRepository.findByEmail(form.getEmail()).isPresent()) {
			throw new DuplicateEmailException("Email already in use: " + form.getEmail());
		}

		Owner owner = new Owner();
		owner.setFirstName(form.getFirstName());
		owner.setLastName(form.getLastName());
		owner.setAddress(form.getAddress());
		owner.setCity(form.getCity());
		owner.setTelephone(form.getTelephone());
		ownerRepository.save(owner);

		User user = new User();
		user.setEmail(form.getEmail());
		user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
		user.setRole(Role.OWNER);
		user.setOwner(owner);
		return userRepository.save(user);
	}

}
