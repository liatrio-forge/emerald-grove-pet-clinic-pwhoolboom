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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.UpcomingVisit;
import org.springframework.samples.petclinic.owner.VisitRepository;
import org.springframework.samples.petclinic.security.Role;
import org.springframework.samples.petclinic.security.User;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatToolsTests {

	@Mock
	private VetRepository vetRepository;

	@Mock
	private PetTypeRepository petTypeRepository;

	@Mock
	private VisitRepository visitRepository;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private ChatTools chatTools;

	@BeforeEach
	void injectClinicInfo() {
		ReflectionTestUtils.setField(chatTools, "clinicInfo", "Test clinic info");
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	private void setOwnerSecurityContext(String email, int ownerId) {
		User user = new User();
		user.setEmail(email);
		user.setRole(Role.OWNER);
		Owner owner = new Owner();
		owner.setId(ownerId);
		user.setOwner(owner);
		when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
		var auth = new UsernamePasswordAuthenticationToken(email, null,
				List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	private void setAdminSecurityContext(String email) {
		var auth = new UsernamePasswordAuthenticationToken(email, null,
				List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	// ---------------------------------------------------------------------------
	// getVeterinarians
	// ---------------------------------------------------------------------------

	@Test
	void getVeterinarians_returnsMappedVetSummaries() {
		Specialty surgery = new Specialty();
		surgery.setName("surgery");

		Vet vet = new Vet();
		vet.setFirstName("James");
		vet.setLastName("Carter");
		vet.addSpecialty(surgery);

		when(vetRepository.findAll()).thenReturn(List.of(vet));

		List<VetSummary> result = chatTools.getVeterinarians();

		assertThat(result).hasSize(1);
		VetSummary summary = result.get(0);
		assertThat(summary.name()).isEqualTo("James Carter");
		assertThat(summary.specialties()).containsExactly("surgery");
		// Compile-time proof: VetSummary has no address or telephone fields.
	}

	// ---------------------------------------------------------------------------
	// getVetsBySpecialty
	// ---------------------------------------------------------------------------

	@Test
	void getVetsBySpecialty_filtersCorrectly() {
		Specialty surgery = new Specialty();
		surgery.setName("surgery");
		Specialty dentistry = new Specialty();
		dentistry.setName("dentistry");

		Vet surgeon = new Vet();
		surgeon.setFirstName("James");
		surgeon.setLastName("Carter");
		surgeon.addSpecialty(surgery);

		Vet dentist = new Vet();
		dentist.setFirstName("Helen");
		dentist.setLastName("Leary");
		dentist.addSpecialty(dentistry);

		when(vetRepository.findAll()).thenReturn(List.of(surgeon, dentist));

		List<VetSummary> result = chatTools.getVetsBySpecialty("SURGERY");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).name()).isEqualTo("James Carter");
	}

	// ---------------------------------------------------------------------------
	// getPetTypes
	// ---------------------------------------------------------------------------

	@Test
	void getPetTypes_returnsTypeNames() {
		PetType dog = new PetType();
		dog.setName("dog");
		PetType cat = new PetType();
		cat.setName("cat");

		when(petTypeRepository.findAll()).thenReturn(List.of(dog, cat));

		List<String> result = chatTools.getPetTypes();

		assertThat(result).containsExactlyInAnyOrder("dog", "cat");
	}

	// ---------------------------------------------------------------------------
	// getUpcomingVisitsForOwner
	// ---------------------------------------------------------------------------

	@Test
	void getUpcomingVisitsForOwner_returnsMatchingVisits() {
		UpcomingVisit franklin = new UpcomingVisit(1, "George Franklin", "Max", LocalDate.of(2026, 3, 1),
				"Annual checkup");
		UpcomingVisit davis = new UpcomingVisit(2, "Betty Davis", "Whiskers", LocalDate.of(2026, 3, 5), "Vaccination");

		when(visitRepository.findUpcomingVisits(any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(franklin, davis));

		List<VisitSummary> result = chatTools.getUpcomingVisitsForOwner("Franklin");

		assertThat(result).hasSize(1);
		VisitSummary summary = result.get(0);
		assertThat(summary.ownerName()).isEqualTo("George Franklin");
		assertThat(summary.petName()).isEqualTo("Max");
		assertThat(summary.visitDate()).isEqualTo(LocalDate.of(2026, 3, 1));
		assertThat(summary.description()).isEqualTo("Annual checkup");
		// Compile-time proof: VisitSummary has no address or telephone fields.
	}

	@Test
	void getUpcomingVisitsForOwner_returnsEmptyListForNullName() {
		assertThat(chatTools.getUpcomingVisitsForOwner(null)).isEmpty();
	}

	@Test
	void getUpcomingVisitsForOwner_returnsEmptyListForBlankName() {
		assertThat(chatTools.getUpcomingVisitsForOwner("  ")).isEmpty();
	}

	// ---------------------------------------------------------------------------
	// getUpcomingVisits
	// ---------------------------------------------------------------------------

	@Test
	void getUpcomingVisits_returnsAtMostTen() {
		List<UpcomingVisit> manyVisits = java.util.stream.IntStream.rangeClosed(1, 15)
			.mapToObj(i -> new UpcomingVisit(i, "Owner " + i, "Pet " + i, LocalDate.of(2026, 3, i), "Visit " + i))
			.toList();

		when(visitRepository.findUpcomingVisits(any(LocalDate.class), any(LocalDate.class))).thenReturn(manyVisits);

		List<VisitSummary> result = chatTools.getUpcomingVisits();

		assertThat(result).hasSize(10);
	}

	// ---------------------------------------------------------------------------
	// getClinicInfo
	// ---------------------------------------------------------------------------

	@Test
	void getClinicInfo_returnsInjectedString() {
		assertThat(chatTools.getClinicInfo()).isEqualTo("Test clinic info");
	}

	// ---------------------------------------------------------------------------
	// Security-aware getUpcomingVisits
	// ---------------------------------------------------------------------------

	@Test
	void getUpcomingVisits_ownerContext_returnsOnlyOwnerVisits() {
		setOwnerSecurityContext("george.franklin@petclinic.com", 1);

		UpcomingVisit georgeVisit = new UpcomingVisit(1, "George Franklin", "Leo", LocalDate.of(2026, 3, 1), "checkup");
		when(visitRepository.findUpcomingVisitsByOwnerId(eq(1), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(georgeVisit));

		List<VisitSummary> result = chatTools.getUpcomingVisits();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).ownerName()).isEqualTo("George Franklin");
	}

	@Test
	void getUpcomingVisits_adminContext_returnsAllVisits() {
		setAdminSecurityContext("admin@petclinic.com");

		UpcomingVisit georgeVisit = new UpcomingVisit(1, "George Franklin", "Leo", LocalDate.of(2026, 3, 1), "checkup");
		UpcomingVisit jeanVisit = new UpcomingVisit(6, "Jean Coleman", "Samantha", LocalDate.of(2026, 3, 2), "shots");
		when(visitRepository.findUpcomingVisits(any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(georgeVisit, jeanVisit));

		List<VisitSummary> result = chatTools.getUpcomingVisits();

		assertThat(result).hasSize(2);
	}

	@Test
	void getUpcomingVisitsForOwner_ownerContext_ignoresOwnerParamAndReturnsOwnVisits() {
		setOwnerSecurityContext("george.franklin@petclinic.com", 1);

		UpcomingVisit georgeVisit = new UpcomingVisit(1, "George Franklin", "Leo", LocalDate.of(2026, 3, 1), "checkup");
		when(visitRepository.findUpcomingVisitsByOwnerId(eq(1), any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(georgeVisit));

		List<VisitSummary> result = chatTools.getUpcomingVisitsForOwner("Coleman");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).ownerName()).isEqualTo("George Franklin");
	}

	@Test
	void getUpcomingVisitsForOwner_adminContext_usesOwnerNameFilter() {
		setAdminSecurityContext("admin@petclinic.com");

		UpcomingVisit georgeVisit = new UpcomingVisit(1, "George Franklin", "Leo", LocalDate.of(2026, 3, 1), "checkup");
		UpcomingVisit jeanVisit = new UpcomingVisit(6, "Jean Coleman", "Samantha", LocalDate.of(2026, 3, 2), "shots");
		when(visitRepository.findUpcomingVisits(any(LocalDate.class), any(LocalDate.class)))
			.thenReturn(List.of(georgeVisit, jeanVisit));

		List<VisitSummary> result = chatTools.getUpcomingVisitsForOwner("Coleman");

		assertThat(result).hasSize(1);
		assertThat(result.get(0).ownerName()).isEqualTo("Jean Coleman");
	}

}
