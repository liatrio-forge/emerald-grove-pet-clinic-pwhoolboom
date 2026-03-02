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

import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.VisitRepository;
import org.springframework.samples.petclinic.security.User;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.samples.petclinic.vet.Specialty;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring AI tool methods that give the LLM read-only access to live clinic data.
 * <p>
 * All return types are purpose-built summary records — never JPA entities — so the set of
 * fields exposed to the model is explicit and privacy-safe. Owner telephone numbers and
 * addresses are never included.
 */
@Component
class ChatTools {

	private final VetRepository vetRepository;

	private final PetTypeRepository petTypeRepository;

	private final VisitRepository visitRepository;

	private final UserRepository userRepository;

	@Value("${petclinic.chat.clinic-info}")
	private String clinicInfo;

	ChatTools(VetRepository vetRepository, PetTypeRepository petTypeRepository, VisitRepository visitRepository,
			UserRepository userRepository) {
		this.vetRepository = vetRepository;
		this.petTypeRepository = petTypeRepository;
		this.visitRepository = visitRepository;
		this.userRepository = userRepository;
	}

	private boolean isOwnerRole() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
	}

	private User getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return null;
		}
		return userRepository.findByEmail(auth.getName()).orElse(null);
	}

	@Tool(description = "List all veterinarians and their specialties")
	List<VetSummary> getVeterinarians() {
		return vetRepository.findAll()
			.stream()
			.map(vet -> new VetSummary(vet.getFirstName() + " " + vet.getLastName(),
					vet.getSpecialties().stream().map(Specialty::getName).toList()))
			.toList();
	}

	@Tool(description = "Find veterinarians by specialty name")
	List<VetSummary> getVetsBySpecialty(String specialty) {
		return getVeterinarians().stream()
			.filter(vs -> vs.specialties().stream().anyMatch(s -> s.equalsIgnoreCase(specialty)))
			.toList();
	}

	@Tool(description = "List all pet types the clinic accepts")
	List<String> getPetTypes() {
		return petTypeRepository.findAll().stream().map(pt -> pt.getName()).toList();
	}

	@Tool(description = "Get upcoming scheduled visits for a named owner")
	List<VisitSummary> getUpcomingVisitsForOwner(String ownerLastName) {
		if (isOwnerRole()) {
			User user = getCurrentUser();
			if (user == null || user.getOwner() == null) {
				return List.of();
			}
			return visitRepository
				.findUpcomingVisitsByOwnerId(user.getOwner().getId(), LocalDate.now(), LocalDate.now().plusYears(1))
				.stream()
				.map(uv -> new VisitSummary(uv.ownerName(), uv.petName(), uv.date(), uv.description()))
				.toList();
		}
		if (ownerLastName == null || ownerLastName.isBlank()) {
			return List.of();
		}
		String normalized = ownerLastName.toLowerCase();
		return visitRepository.findUpcomingVisits(LocalDate.now(), LocalDate.now().plusYears(1))
			.stream()
			.filter(uv -> uv.ownerName().toLowerCase().contains(normalized))
			.map(uv -> new VisitSummary(uv.ownerName(), uv.petName(), uv.date(), uv.description()))
			.toList();
	}

	@Tool(description = "Get the next upcoming clinic visits across all owners")
	List<VisitSummary> getUpcomingVisits() {
		if (isOwnerRole()) {
			User user = getCurrentUser();
			if (user == null || user.getOwner() == null) {
				return List.of();
			}
			return visitRepository
				.findUpcomingVisitsByOwnerId(user.getOwner().getId(), LocalDate.now(), LocalDate.now().plusYears(1))
				.stream()
				.map(uv -> new VisitSummary(uv.ownerName(), uv.petName(), uv.date(), uv.description()))
				.toList();
		}
		return visitRepository.findUpcomingVisits(LocalDate.now(), LocalDate.now().plusYears(1))
			.stream()
			.limit(10)
			.map(uv -> new VisitSummary(uv.ownerName(), uv.petName(), uv.date(), uv.description()))
			.toList();
	}

	@Tool(description = "Get general clinic information such as hours and services")
	String getClinicInfo() {
		return clinicInfo;
	}

}
