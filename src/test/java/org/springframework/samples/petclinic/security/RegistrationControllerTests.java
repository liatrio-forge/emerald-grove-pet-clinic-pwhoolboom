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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(RegistrationController.class)
@DisabledInNativeImage
@DisabledInAotMode
@Import(WebMvcTestSecurityConfig.class)
class RegistrationControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RegistrationService registrationService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private OwnerAuthenticationSuccessHandler ownerAuthenticationSuccessHandler;

	@Test
	void getRegister_returns200AndRegistrationView() throws Exception {
		mockMvc.perform(get("/register"))
			.andExpect(status().isOk())
			.andExpect(view().name("security/register"))
			.andExpect(model().attributeExists("registrationForm"));
	}

	@Test
	void postRegister_validData_redirectsToOwnerPage() throws Exception {
		User savedUser = new User();
		Owner owner = new Owner();
		owner.setId(1);
		savedUser.setOwner(owner);
		given(registrationService.register(any(RegistrationForm.class))).willReturn(savedUser);

		mockMvc
			.perform(post("/register").with(csrf())
				.param("email", "test@example.com")
				.param("password", "secret123")
				.param("firstName", "Test")
				.param("lastName", "User")
				.param("address", "123 Main St")
				.param("city", "Testville")
				.param("telephone", "1234567890"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrlPattern("/owners/*"));
	}

	@Test
	void postRegister_duplicateEmail_reRendersFormWithError() throws Exception {
		given(registrationService.register(any(RegistrationForm.class)))
			.willThrow(new DuplicateEmailException("Email already in use"));

		mockMvc
			.perform(post("/register").with(csrf())
				.param("email", "existing@example.com")
				.param("password", "secret123")
				.param("firstName", "Test")
				.param("lastName", "User")
				.param("address", "123 Main St")
				.param("city", "Testville")
				.param("telephone", "1234567890"))
			.andExpect(status().isOk())
			.andExpect(view().name("security/register"))
			.andExpect(model().hasErrors());
	}

	@Test
	void postRegister_blankPassword_reRendersFormWithValidationError() throws Exception {
		mockMvc
			.perform(post("/register").with(csrf())
				.param("email", "test@example.com")
				.param("password", "")
				.param("firstName", "Test")
				.param("lastName", "User")
				.param("address", "123 Main St")
				.param("city", "Testville")
				.param("telephone", "1234567890"))
			.andExpect(status().isOk())
			.andExpect(view().name("security/register"))
			.andExpect(model().attributeHasFieldErrors("registrationForm", "password"));
	}

}
