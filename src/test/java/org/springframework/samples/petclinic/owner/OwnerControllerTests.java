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

package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.samples.petclinic.security.Role;
import org.springframework.samples.petclinic.security.User;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.security.OwnerAuthenticationSuccessHandler;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.samples.petclinic.security.WebMvcTestSecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(OwnerController.class)
@DisabledInNativeImage
@DisabledInAotMode
@WithMockUser
@Import(WebMvcTestSecurityConfig.class)
class OwnerControllerTests {

	private static final int TEST_OWNER_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private OwnerAuthenticationSuccessHandler ownerAuthenticationSuccessHandler;

	private Owner george() {
		Owner george = new Owner();
		george.setId(TEST_OWNER_ID);
		george.setFirstName("George");
		george.setLastName("Franklin");
		george.setAddress("110 W. Liberty St.");
		george.setCity("Madison");
		george.setTelephone("6085551023");
		Pet max = new Pet();
		PetType dog = new PetType();
		dog.setName("dog");
		max.setType(dog);
		max.setName("Max");
		max.setBirthDate(LocalDate.now());
		george.addPet(max);
		max.setId(1);
		return george;
	}

	@BeforeEach
	void setup() {

		Owner george = george();
		given(this.owners.findByLastNameStartingWith(eq("Franklin"), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george)));

		// Default stub for multi-field search: single result → redirect
		given(this.owners.findByFilters(any(), any(), any(), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george)));

		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george));

		given(this.owners.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndTelephone(anyString(), anyString(),
				anyString()))
			.willReturn(Optional.empty());

		Visit visit = new Visit();
		visit.setDate(LocalDate.now());
		george.getPet("Max").getVisits().add(visit);

	}

	@Test
	void testInitCreationForm() throws Exception {
		mockMvc.perform(get("/owners/new"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("owner"))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void testProcessCreationFormSuccess() throws Exception {
		mockMvc
			.perform(post("/owners/new").with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1316761638"))
			.andExpect(status().is3xxRedirection());
	}

	@Test
	void testProcessCreationFormDuplicateOwner() throws Exception {
		// Arrange
		given(this.owners.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndTelephone("Joe", "Bloggs", "1316761638"))
			.willReturn(Optional.of(george()));

		// Act & Assert
		mockMvc
			.perform(post("/owners/new").with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1316761638"))
			.andExpect(status().isOk())
			.andExpect(model().hasErrors())
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void testProcessCreationFormDbConstraintViolation() throws Exception {
		// Arrange: application-level check passes but DB save throws due to race
		// condition
		willThrow(new DataIntegrityViolationException("unique constraint violation")).given(this.owners)
			.save(any(Owner.class));

		// Act & Assert
		mockMvc
			.perform(post("/owners/new").with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1316761638"))
			.andExpect(status().isOk())
			.andExpect(model().hasErrors())
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void testProcessCreationFormHasErrors() throws Exception {
		mockMvc
			.perform(post("/owners/new").with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("city", "London"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("owner"))
			.andExpect(model().attributeHasFieldErrors("owner", "address"))
			.andExpect(model().attributeHasFieldErrors("owner", "telephone"))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void testInitFindForm() throws Exception {
		mockMvc.perform(get("/owners/find"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("owner"))
			.andExpect(view().name("owners/findOwners"));
	}

	@Test
	void testProcessFindFormSuccess() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1")).andExpect(status().isOk()).andExpect(view().name("owners/ownersList"));
	}

	@Test
	void testProcessFindFormByLastName() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByFilters(eq("Franklin"), any(), any(), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param("lastName", "Franklin"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessFindFormNoOwnersFound() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of());
		when(this.owners.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param("lastName", "Unknown Surname"))
			.andExpect(status().isOk())
			.andExpect(model().hasErrors())
			.andExpect(view().name("owners/findOwners"));
	}

	@Test
	void testProcessFindFormByTelephone() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByFilters(any(), eq("6085551023"), any(), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param("telephone", "6085551023"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testProcessFindFormByCityPrefix() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(any(), any(), eq("Mad"), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param("city", "Mad"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownersList"))
			.andExpect(model().attributeExists("listOwners"));
	}

	@Test
	void testProcessFindFormByAllThreeFilters() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByFilters(eq("Franklin"), eq("6085551023"), eq("Mad"), any(Pageable.class)))
			.thenReturn(tasks);
		mockMvc
			.perform(get("/owners?page=1").param("lastName", "Franklin")
				.param("telephone", "6085551023")
				.param("city", "Mad"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/" + TEST_OWNER_ID));
	}

	@Test
	void testPaginationModelIncludesLastNameWhenFilterActive() throws Exception {
		// Arrange
		Page<Owner> multiPage = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(eq("Franklin"), any(), any(), any(Pageable.class))).thenReturn(multiPage);

		// Act & Assert
		mockMvc.perform(get("/owners?page=1").param("lastName", "Franklin"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownersList"))
			.andExpect(model().attribute("lastName", "Franklin"));
	}

	@Test
	void testPaginationModelHasNullLastNameWhenNoFilterActive() throws Exception {
		// Arrange
		Page<Owner> multiPage = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(multiPage);

		// Act & Assert
		mockMvc.perform(get("/owners?page=1"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownersList"))
			.andExpect(model().attribute("lastName", nullValue()));
	}

	@Test
	void testPaginationLinksIncludeLastNameWhenFilterActive() throws Exception {
		// Arrange
		Page<Owner> multiPage = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(eq("Franklin"), any(), any(), any(Pageable.class))).thenReturn(multiPage);

		// Act & Assert
		mockMvc.perform(get("/owners?page=1").param("lastName", "Franklin"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("lastName=Franklin")));
	}

	@Test
	void testProcessFindFormInvalidTelephone() throws Exception {
		mockMvc.perform(get("/owners?page=1").param("telephone", "123"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("owner", "telephone"))
			.andExpect(model().attributeHasFieldErrorCode("owner", "telephone", "telephone.invalid"))
			.andExpect(view().name("owners/findOwners"));
	}

	@Test
	void testProcessFindFormEmptyTelephoneNoError() throws Exception {
		Page<Owner> empty = new PageImpl<>(List.of());
		when(this.owners.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(empty);
		mockMvc.perform(get("/owners?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeDoesNotExist("owner.telephone"))
			.andExpect(view().name("owners/findOwners"));
	}

	@Test
	void testProcessFindFormValidTelephoneNoError() throws Exception {
		mockMvc.perform(get("/owners?page=1").param("telephone", "6085551023")).andExpect(status().is3xxRedirection());
	}

	@Test
	void testPaginationModelIncludesTelephoneWhenFilterActive() throws Exception {
		Page<Owner> multiPage = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(any(), eq("6085551023"), any(), any(Pageable.class))).thenReturn(multiPage);

		mockMvc.perform(get("/owners?page=1").param("telephone", "6085551023"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownersList"))
			.andExpect(model().attribute("telephone", "6085551023"));
	}

	@Test
	void testPaginationModelIncludesCityWhenFilterActive() throws Exception {
		Page<Owner> multiPage = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(any(), any(), eq("Mad"), any(Pageable.class))).thenReturn(multiPage);

		mockMvc.perform(get("/owners?page=1").param("city", "Mad"))
			.andExpect(status().isOk())
			.andExpect(view().name("owners/ownersList"))
			.andExpect(model().attribute("city", "Mad"));
	}

	@Test
	void testPaginationLinksIncludeTelephoneAndCityWhenFiltersActive() throws Exception {
		Page<Owner> multiPage = new PageImpl<>(List.of(george(), new Owner()), PageRequest.of(0, 5), 10);
		when(this.owners.findByFilters(eq("Franklin"), eq("6085551023"), eq("Mad"), any(Pageable.class)))
			.thenReturn(multiPage);

		mockMvc
			.perform(get("/owners?page=1").param("lastName", "Franklin")
				.param("telephone", "6085551023")
				.param("city", "Mad"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("telephone=6085551023")))
			.andExpect(content().string(containsString("city=Mad")));
	}

	@Test
	void testInitUpdateOwnerForm() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}/edit", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("owner"))
			.andExpect(model().attribute("owner", hasProperty("lastName", is("Franklin"))))
			.andExpect(model().attribute("owner", hasProperty("firstName", is("George"))))
			.andExpect(model().attribute("owner", hasProperty("address", is("110 W. Liberty St."))))
			.andExpect(model().attribute("owner", hasProperty("city", is("Madison"))))
			.andExpect(model().attribute("owner", hasProperty("telephone", is("6085551023"))))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void testProcessUpdateOwnerFormSuccess() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID).with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1616291589"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessUpdateOwnerFormUnchangedSuccess() throws Exception {
		mockMvc.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void testProcessUpdateOwnerFormHasErrors() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID).with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "")
				.param("telephone", ""))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("owner"))
			.andExpect(model().attributeHasFieldErrors("owner", "address"))
			.andExpect(model().attributeHasFieldErrors("owner", "telephone"))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void testShowOwner() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attribute("owner", hasProperty("lastName", is("Franklin"))))
			.andExpect(model().attribute("owner", hasProperty("firstName", is("George"))))
			.andExpect(model().attribute("owner", hasProperty("address", is("110 W. Liberty St."))))
			.andExpect(model().attribute("owner", hasProperty("city", is("Madison"))))
			.andExpect(model().attribute("owner", hasProperty("telephone", is("6085551023"))))
			.andExpect(model().attribute("owner", hasProperty("pets", not(empty()))))
			.andExpect(model().attribute("owner",
					hasProperty("pets", hasItem(hasProperty("visits", hasSize(greaterThan(0)))))))
			.andExpect(view().name("owners/ownerDetails"));
	}

	@Test
	void testProcessUpdateOwnerFormDuplicateOwner() throws Exception {
		// Arrange: a second, different owner already has the target name+telephone
		Owner conflicting = new Owner();
		conflicting.setId(2);
		conflicting.setFirstName("Joe");
		conflicting.setLastName("Bloggs");
		conflicting.setAddress("99 Other St.");
		conflicting.setCity("London");
		conflicting.setTelephone("1616291589");

		given(this.owners.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndTelephone("Joe", "Bloggs", "1616291589"))
			.willReturn(Optional.of(conflicting));

		// Act & Assert: editing owner with TEST_OWNER_ID (1) to a name+telephone owned by
		// id=2 must be blocked
		mockMvc
			.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID).with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1616291589"))
			.andExpect(status().isOk())
			.andExpect(model().hasErrors())
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void testProcessUpdateOwnerFormDbConstraintViolation() throws Exception {
		// Arrange: application-level check passes but DB save throws due to race
		// condition
		willThrow(new DataIntegrityViolationException("unique constraint violation")).given(this.owners)
			.save(any(Owner.class));

		// Act & Assert: editing owner with TEST_OWNER_ID (1) must surface the DB error as
		// a form error
		mockMvc
			.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID).with(csrf())
				.param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1616291589"))
			.andExpect(status().isOk())
			.andExpect(model().hasErrors())
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	public void testProcessUpdateOwnerFormWithIdMismatch() throws Exception {
		int pathOwnerId = 1;

		Owner owner = new Owner();
		owner.setId(2);
		owner.setFirstName("John");
		owner.setLastName("Doe");
		owner.setAddress("Center Street");
		owner.setCity("New York");
		owner.setTelephone("0123456789");

		when(owners.findById(pathOwnerId)).thenReturn(Optional.of(owner));

		mockMvc
			.perform(MockMvcRequestBuilders.post("/owners/{ownerId}/edit", pathOwnerId)
				.with(csrf())
				.flashAttr("owner", owner))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owners/" + pathOwnerId + "/edit"))
			.andExpect(flash().attributeExists("error"));
	}

	@Test
	void testShowNonExistentOwner() throws Exception {
		given(this.owners.findById(99999)).willReturn(Optional.empty());
		mockMvc.perform(get("/owners/99999")).andExpect(status().isNotFound());
	}

	@Test
	void testExportOwnersCsvNoFilter() throws Exception {
		// Arrange
		when(this.owners.findByLastNameStartingWith(eq(""), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(george())));

		// Act & Assert
		mockMvc.perform(get("/owners.csv"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/csv"))
			.andExpect(content()
				.string(org.hamcrest.Matchers.containsString("id,firstName,lastName,address,city,telephone")));
	}

	@Test
	void testExportOwnersCsvWithLastNameFilter() throws Exception {
		// Arrange
		when(this.owners.findByLastNameStartingWith(eq("Franklin"), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of(george())));

		// Act & Assert
		mockMvc.perform(get("/owners.csv").param("lastName", "Franklin"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/csv"))
			.andExpect(content().string(org.hamcrest.Matchers
				.containsString("\"1\",\"George\",\"Franklin\",\"110 W. Liberty St.\",\"Madison\",\"6085551023\"")));
	}

	@Test
	void testExportOwnersCsvEmptyResults() throws Exception {
		// Arrange
		when(this.owners.findByLastNameStartingWith(eq("Unknown"), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		// Act & Assert
		mockMvc.perform(get("/owners.csv").param("lastName", "Unknown"))
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith("text/csv"))
			.andExpect(content().string("id,firstName,lastName,address,city,telephone\n"));
	}

	// ---------------------------------------------------------------------------
	// Access control: OWNER role
	// ---------------------------------------------------------------------------

	@Test
	@WithMockUser(username = "george.franklin@petclinic.com", roles = "OWNER")
	void testShowOwner_ownerAccessingOwnProfile_returns200() throws Exception {
		User georgeUser = new User();
		georgeUser.setEmail("george.franklin@petclinic.com");
		georgeUser.setRole(Role.OWNER);
		georgeUser.setOwner(george());
		given(userRepository.findByEmail("george.franklin@petclinic.com")).willReturn(Optional.of(georgeUser));

		mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID)).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "george.franklin@petclinic.com", roles = "OWNER")
	void testShowOwner_ownerAccessingOtherOwnerProfile_returns403() throws Exception {
		User georgeUser = new User();
		georgeUser.setEmail("george.franklin@petclinic.com");
		georgeUser.setRole(Role.OWNER);
		georgeUser.setOwner(george());
		given(userRepository.findByEmail("george.franklin@petclinic.com")).willReturn(Optional.of(georgeUser));

		Owner betty = new Owner();
		betty.setId(2);
		given(this.owners.findById(2)).willReturn(Optional.of(betty));

		mockMvc.perform(get("/owners/{ownerId}", 2)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "george.franklin@petclinic.com", roles = "OWNER")
	void testInitUpdateOwnerForm_ownerAccessingOtherOwner_returns403() throws Exception {
		User georgeUser = new User();
		georgeUser.setEmail("george.franklin@petclinic.com");
		georgeUser.setRole(Role.OWNER);
		georgeUser.setOwner(george());
		given(userRepository.findByEmail("george.franklin@petclinic.com")).willReturn(Optional.of(georgeUser));

		Owner betty = new Owner();
		betty.setId(2);
		given(this.owners.findById(2)).willReturn(Optional.of(betty));

		mockMvc.perform(get("/owners/{ownerId}/edit", 2)).andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(username = "george.franklin@petclinic.com", roles = "OWNER")
	void testProcessFindForm_ownerRoleUser_seesOnlyOwnRecord() throws Exception {
		User georgeUser = new User();
		georgeUser.setEmail("george.franklin@petclinic.com");
		georgeUser.setRole(Role.OWNER);
		georgeUser.setOwner(george());
		given(userRepository.findByEmail("george.franklin@petclinic.com")).willReturn(Optional.of(georgeUser));

		Owner betty = new Owner();
		betty.setId(2);
		betty.setFirstName("Betty");
		betty.setLastName("Davis");
		Page<Owner> allOwners = new PageImpl<>(List.of(george(), betty), PageRequest.of(0, 5), 2);
		when(this.owners.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(allOwners);

		mockMvc.perform(get("/owners?page=1"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owners/" + TEST_OWNER_ID));
	}

	// ---------------------------------------------------------------------------
	// Access control: ADMIN role
	// ---------------------------------------------------------------------------

	@Test
	@WithMockUser(username = "admin@petclinic.com", roles = "ADMIN")
	void testShowOwner_adminAccessesAnyProfile_returns200() throws Exception {
		User adminUser = new User();
		adminUser.setEmail("admin@petclinic.com");
		adminUser.setRole(Role.ADMIN);
		given(userRepository.findByEmail("admin@petclinic.com")).willReturn(Optional.of(adminUser));

		mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID)).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "admin@petclinic.com", roles = "ADMIN")
	void testShowOwner_canEditIsTrueForAdmin() throws Exception {
		User adminUser = new User();
		adminUser.setEmail("admin@petclinic.com");
		adminUser.setRole(Role.ADMIN);
		given(userRepository.findByEmail("admin@petclinic.com")).willReturn(Optional.of(adminUser));

		mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attribute("canEdit", true));
	}

	@Test
	@WithMockUser(username = "admin@petclinic.com", roles = "ADMIN")
	void testProcessFindForm_adminSeesAllOwners() throws Exception {
		User adminUser = new User();
		adminUser.setEmail("admin@petclinic.com");
		adminUser.setRole(Role.ADMIN);
		given(userRepository.findByEmail("admin@petclinic.com")).willReturn(Optional.of(adminUser));

		Owner betty = new Owner();
		betty.setId(2);
		betty.setFirstName("Betty");
		betty.setLastName("Davis");
		Page<Owner> allOwners = new PageImpl<>(List.of(george(), betty), PageRequest.of(0, 5), 2);
		when(this.owners.findByFilters(any(), any(), any(), any(Pageable.class))).thenReturn(allOwners);

		mockMvc.perform(get("/owners?page=1")).andExpect(status().isOk()).andExpect(view().name("owners/ownersList"));
	}

}
