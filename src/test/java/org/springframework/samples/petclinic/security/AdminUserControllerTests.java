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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminUserController.class)
@DisabledInNativeImage
@DisabledInAotMode
@WithMockUser(username = "admin@petclinic.com", roles = "ADMIN")
@Import(WebMvcTestSecurityConfig.class)
class AdminUserControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private OwnerRepository ownerRepository;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@MockitoBean
	private OwnerAuthenticationSuccessHandler ownerAuthenticationSuccessHandler;

	private User adminUser;

	private User ownerUser;

	@BeforeEach
	void setup() {
		// Current admin user (id=1)
		adminUser = new User();
		adminUser.setId(1);
		adminUser.setEmail("admin@petclinic.com");
		adminUser.setPasswordHash("hashedpw");
		adminUser.setRole(Role.ADMIN);
		adminUser.setOwner(null);

		// An owner-role user (id=2) with a linked owner
		Owner linkedOwner = new Owner();
		linkedOwner.setId(10);
		linkedOwner.setFirstName("Test");
		linkedOwner.setLastName("User");
		linkedOwner.setAddress("123 Main St");
		linkedOwner.setCity("Testville");
		linkedOwner.setTelephone("1234567890");

		ownerUser = new User();
		ownerUser.setId(2);
		ownerUser.setEmail("test@example.com");
		ownerUser.setPasswordHash("hashedpw2");
		ownerUser.setRole(Role.OWNER);
		ownerUser.setOwner(linkedOwner);

		// Stub findAll for list
		given(userRepository.findAll()).willReturn(List.of(adminUser, ownerUser));

		// Stub findByEmail for admin lookup
		given(userRepository.findByEmail("admin@petclinic.com")).willReturn(Optional.of(adminUser));

		// Stub findByEmail for others: empty by default
		given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(ownerUser));
		given(userRepository.findByEmail(anyString())).willAnswer(invocation -> {
			String email = invocation.getArgument(0);
			if ("admin@petclinic.com".equals(email)) {
				return Optional.of(adminUser);
			}
			if ("test@example.com".equals(email)) {
				return Optional.of(ownerUser);
			}
			return Optional.empty();
		});

		// Stub findById
		given(userRepository.findById(1)).willReturn(Optional.of(adminUser));
		given(userRepository.findById(2)).willReturn(Optional.of(ownerUser));
		given(userRepository.findById(anyInt())).willAnswer(invocation -> {
			int id = invocation.getArgument(0);
			if (id == 1) {
				return Optional.of(adminUser);
			}
			if (id == 2) {
				return Optional.of(ownerUser);
			}
			return Optional.empty();
		});
	}

	// ============================================================
	// Task 1 - User List
	// ============================================================

	@Test
	void testGetUserList_asAdmin_returns200() throws Exception {
		mockMvc.perform(get("/admin/users"))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/userList"))
			.andExpect(model().attributeExists("users"));
	}

	@Test
	@WithMockUser(roles = "OWNER")
	void testGetUserList_asOwner_returns403() throws Exception {
		mockMvc.perform(get("/admin/users")).andExpect(status().isForbidden());
	}

	@Test
	@WithAnonymousUser
	void testGetUserList_unauthenticated_redirectsToLogin() throws Exception {
		mockMvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection());
	}

	// ============================================================
	// Task 2 - Create New Admin User
	// ============================================================

	@Test
	void testGetNewAdminForm_returns200() throws Exception {
		mockMvc.perform(get("/admin/users/new"))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/createAdminUserForm"))
			.andExpect(model().attributeExists("adminUserForm"));
	}

	@Test
	void testPostNewAdmin_validData_redirectsToList() throws Exception {
		given(userRepository.findByEmail("newadmin@example.com")).willReturn(Optional.empty());

		mockMvc
			.perform(post("/admin/users/new").with(csrf())
				.param("email", "newadmin@example.com")
				.param("password", "secret123"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/users"));
	}

	@Test
	void testPostNewAdmin_duplicateEmail_showsFormError() throws Exception {
		given(userRepository.findByEmail("admin@petclinic.com")).willReturn(Optional.of(adminUser));

		mockMvc
			.perform(post("/admin/users/new").with(csrf())
				.param("email", "admin@petclinic.com")
				.param("password", "secret123"))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/createAdminUserForm"))
			.andExpect(model().attributeHasErrors("adminUserForm"));
	}

	@Test
	void testPostNewAdmin_blankPassword_showsFormError() throws Exception {
		mockMvc
			.perform(post("/admin/users/new").with(csrf()).param("email", "newadmin@example.com").param("password", ""))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/createAdminUserForm"))
			.andExpect(model().attributeHasFieldErrors("adminUserForm", "password"));
	}

	// ============================================================
	// Task 3 - Edit User
	// ============================================================

	@Test
	void testGetEditForm_asAdmin_returns200() throws Exception {
		mockMvc.perform(get("/admin/users/2/edit"))
			.andExpect(status().isOk())
			.andExpect(view().name("admin/editUserForm"))
			.andExpect(model().attributeExists("userEditForm"));
	}

	@Test
	void testGetEditForm_ownAccount_returns403() throws Exception {
		mockMvc.perform(get("/admin/users/1/edit")).andExpect(status().isForbidden());
	}

	@Test
	void testPostEdit_validData_redirectsToList() throws Exception {
		mockMvc
			.perform(post("/admin/users/2/edit").with(csrf())
				.param("id", "2")
				.param("email", "test@example.com")
				.param("password", "")
				.param("role", "OWNER")
				.param("firstName", "Test")
				.param("lastName", "User")
				.param("address", "123 Main St")
				.param("city", "Testville")
				.param("telephone", "1234567890"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/users"));
	}

	@Test
	void testPostEdit_blankPassword_preservesPasswordHash() throws Exception {
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		mockMvc
			.perform(post("/admin/users/2/edit").with(csrf())
				.param("id", "2")
				.param("email", "test@example.com")
				.param("password", "")
				.param("role", "OWNER")
				.param("firstName", "Test")
				.param("lastName", "User")
				.param("address", "123 Main St")
				.param("city", "Testville")
				.param("telephone", "1234567890"))
			.andExpect(status().is3xxRedirection());

		verify(userRepository).save(userCaptor.capture());
		User saved = userCaptor.getValue();
		assert "hashedpw2".equals(saved.getPasswordHash()) : "Password hash should be unchanged";
	}

	@Test
	void testPostEdit_ownerToAdmin_unlinksOwner() throws Exception {
		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

		mockMvc
			.perform(post("/admin/users/2/edit").with(csrf())
				.param("id", "2")
				.param("email", "test@example.com")
				.param("password", "")
				.param("role", "ADMIN"))
			.andExpect(status().is3xxRedirection());

		verify(userRepository).save(userCaptor.capture());
		User saved = userCaptor.getValue();
		assert saved.getOwner() == null : "Owner should be unlinked when role changes to ADMIN";
	}

	@Test
	void testPostEdit_ownAccount_returns403() throws Exception {
		mockMvc
			.perform(post("/admin/users/1/edit").with(csrf())
				.param("id", "1")
				.param("email", "admin@petclinic.com")
				.param("password", "")
				.param("role", "ADMIN"))
			.andExpect(status().isForbidden());
	}

	@Test
	void testPostEdit_duplicateEmail_showsFormError() throws Exception {
		// ownerUser tries to update email to one that belongs to adminUser
		given(userRepository.findByEmail("admin@petclinic.com")).willReturn(Optional.of(adminUser));

		mockMvc
			.perform(post("/admin/users/2/edit").with(csrf())
				.param("id", "2")
				.param("email", "admin@petclinic.com")
				.param("password", "")
				.param("role", "OWNER")
				.param("firstName", "Test")
				.param("lastName", "User")
				.param("address", "123 Main St")
				.param("city", "Testville")
				.param("telephone", "1234567890"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("userEditForm"));
	}

	// ============================================================
	// Task 4 - Delete User
	// ============================================================

	@Test
	void testPostDelete_asAdmin_redirectsToList() throws Exception {
		mockMvc.perform(post("/admin/users/2/delete").with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/admin/users"));
	}

	@Test
	void testPostDelete_ownAccount_returns403() throws Exception {
		mockMvc.perform(post("/admin/users/1/delete").with(csrf())).andExpect(status().isForbidden());
	}

	@Test
	void testPostDelete_withoutCascade_preservesOwner() throws Exception {
		mockMvc.perform(post("/admin/users/2/delete").with(csrf()).param("cascadeOwner", "false"))
			.andExpect(status().is3xxRedirection());

		verify(ownerRepository, never()).delete(any(Owner.class));
		verify(userRepository).deleteById(2);
	}

	@Test
	void testPostDelete_withCascade_deletesOwner() throws Exception {
		mockMvc.perform(post("/admin/users/2/delete").with(csrf()).param("cascadeOwner", "true"))
			.andExpect(status().is3xxRedirection());

		verify(ownerRepository).delete(ownerUser.getOwner());
		verify(userRepository).deleteById(2);
	}

}
