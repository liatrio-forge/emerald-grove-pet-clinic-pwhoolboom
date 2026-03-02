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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.hasSize;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.security.OwnerAuthenticationSuccessHandler;
import org.springframework.samples.petclinic.security.Role;
import org.springframework.samples.petclinic.security.User;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.samples.petclinic.security.WebMvcTestSecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Test class for {@link UpcomingVisitsController}
 */
@WebMvcTest(UpcomingVisitsController.class)
@DisabledInNativeImage
@DisabledInAotMode
@WithMockUser
@Import(WebMvcTestSecurityConfig.class)
class UpcomingVisitsControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VisitRepository visitRepository;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private OwnerAuthenticationSuccessHandler ownerAuthenticationSuccessHandler;

	private UpcomingVisit upcomingVisit() {
		return new UpcomingVisit(1, "George Franklin", "Samantha", LocalDate.now().plusDays(1), "rabies shot");
	}

	@Test
	void testShowUpcomingVisitsDefault() throws Exception {
		given(this.visitRepository.findUpcomingVisits(any(), any())).willReturn(List.of(upcomingVisit()));

		mockMvc.perform(get("/visits/upcoming"))
			.andExpect(status().isOk())
			.andExpect(view().name("visits/upcomingVisits"))
			.andExpect(model().attributeExists("upcomingVisits"));
	}

	@Test
	void testShowUpcomingVisitsWithDaysParam() throws Exception {
		given(this.visitRepository.findUpcomingVisits(any(), any())).willReturn(List.of(upcomingVisit()));

		mockMvc.perform(get("/visits/upcoming").param("days", "14"))
			.andExpect(status().isOk())
			.andExpect(view().name("visits/upcomingVisits"))
			.andExpect(model().attributeExists("upcomingVisits"));
	}

	@Test
	void testShowUpcomingVisitsInvalidDaysZero() throws Exception {
		mockMvc.perform(get("/visits/upcoming").param("days", "0"))
			.andExpect(status().isOk())
			.andExpect(view().name("visits/upcomingVisits"))
			.andExpect(model().attributeExists("errorMessage"))
			.andExpect(model().attributeDoesNotExist("upcomingVisits"));
	}

	@Test
	void testShowUpcomingVisitsInvalidDays366() throws Exception {
		mockMvc.perform(get("/visits/upcoming").param("days", "366"))
			.andExpect(status().isOk())
			.andExpect(view().name("visits/upcomingVisits"))
			.andExpect(model().attributeExists("errorMessage"))
			.andExpect(model().attributeDoesNotExist("upcomingVisits"));
	}

	@Test
	void testShowUpcomingVisitsEmptyState() throws Exception {
		given(this.visitRepository.findUpcomingVisits(any(), any())).willReturn(List.of());

		mockMvc.perform(get("/visits/upcoming"))
			.andExpect(status().isOk())
			.andExpect(view().name("visits/upcomingVisits"))
			.andExpect(model().attributeExists("upcomingVisits"))
			.andExpect(model().attributeDoesNotExist("errorMessage"));
	}

	@Test
	@WithMockUser(username = "george.franklin@petclinic.com", roles = "OWNER")
	void testShowUpcomingVisits_ownerRoleUser_seesOnlyOwnPets() throws Exception {
		User georgeUser = new User();
		georgeUser.setEmail("george.franklin@petclinic.com");
		georgeUser.setRole(Role.OWNER);
		Owner george = new Owner();
		george.setId(1);
		georgeUser.setOwner(george);
		given(userRepository.findByEmail("george.franklin@petclinic.com")).willReturn(Optional.of(georgeUser));

		UpcomingVisit georgeVisit = new UpcomingVisit(1, "George Franklin", "Samantha", LocalDate.now().plusDays(1),
				"rabies shot");
		given(visitRepository.findUpcomingVisitsByOwnerId(eq(1), any(), any())).willReturn(List.of(georgeVisit));

		mockMvc.perform(get("/visits/upcoming"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("upcomingVisits", hasSize(1)));
	}

	@Test
	@WithMockUser(username = "admin@petclinic.com", roles = "ADMIN")
	void testShowUpcomingVisits_adminSeesAllVisits() throws Exception {
		UpcomingVisit visit1 = new UpcomingVisit(1, "George Franklin", "Leo", LocalDate.now().plusDays(1), "checkup");
		UpcomingVisit visit2 = new UpcomingVisit(6, "Jean Coleman", "Samantha", LocalDate.now().plusDays(2), "shots");
		given(visitRepository.findUpcomingVisits(any(), any())).willReturn(List.of(visit1, visit2));

		mockMvc.perform(get("/visits/upcoming"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("upcomingVisits", hasSize(2)));
	}

}
