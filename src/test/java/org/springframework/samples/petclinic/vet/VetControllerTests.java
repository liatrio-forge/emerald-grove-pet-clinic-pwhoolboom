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

package org.springframework.samples.petclinic.vet;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.context.annotation.Import;
import org.springframework.samples.petclinic.security.OwnerAuthenticationSuccessHandler;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.samples.petclinic.security.WebMvcTestSecurityConfig;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Test class for the {@link VetController}
 */

@WebMvcTest(VetController.class)
@DisabledInNativeImage
@DisabledInAotMode
@WithMockUser
@Import(WebMvcTestSecurityConfig.class)
class VetControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private OwnerAuthenticationSuccessHandler ownerAuthenticationSuccessHandler;

	private Vet james() {
		Vet james = new Vet();
		james.setFirstName("James");
		james.setLastName("Carter");
		james.setId(1);
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setFirstName("Helen");
		helen.setLastName("Leary");
		helen.setId(2);
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		helen.addSpecialty(radiology);
		return helen;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(Lists.newArrayList(james(), helen()));
		given(this.vets.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList(james(), helen())));

	}

	@Test
	void testShowVetListHtml() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/vets.html?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listVets"))
			.andExpect(view().name("vets/vetList"));

	}

	@Test
	void testShowVetListHtmlNoFilter() throws Exception {
		mockMvc.perform(get("/vets.html?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listSpecialties"))
			.andExpect(model().attribute("specialty", ""))
			.andExpect(view().name("vets/vetList"));
	}

	@Test
	void testShowVetListHtmlFilterBySpecialty() throws Exception {
		given(this.vets.findBySpecialtyName(eq("radiology"), any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList(helen())));

		mockMvc.perform(get("/vets.html?specialty=radiology"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("specialty", "radiology"))
			.andExpect(model().attributeExists("listSpecialties"))
			.andExpect(view().name("vets/vetList"));
	}

	@Test
	void testShowVetListHtmlFilterByNone() throws Exception {
		given(this.vets.findWithNoSpecialties(any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList(james())));

		mockMvc.perform(get("/vets.html?specialty=none"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("specialty", "none"))
			.andExpect(model().attributeExists("listSpecialties"))
			.andExpect(view().name("vets/vetList"));
	}

	@Test
	void testShowVetListHtmlFilterByUnknownSpecialty() throws Exception {
		given(this.vets.findBySpecialtyName(eq("unknownXYZ"), any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList()));

		mockMvc.perform(get("/vets.html?specialty=unknownXYZ"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("specialty", "unknownXYZ"))
			.andExpect(model().attribute("totalItems", 0L))
			.andExpect(view().name("vets/vetList"));
	}

	@Test
	void testShowResourcesVetList() throws Exception {
		ResultActions actions = mockMvc.perform(get("/vets").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		actions.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.vetList[0].id").value(1));
	}

}
