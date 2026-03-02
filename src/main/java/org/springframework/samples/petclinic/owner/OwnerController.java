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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.security.User;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.samples.petclinic.system.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import jakarta.validation.Valid;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Wick Dynex
 */
@Controller
class OwnerController {

	private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";

	private final OwnerRepository owners;

	private final UserRepository userRepository;

	public OwnerController(OwnerRepository owners, UserRepository userRepository) {
		this.owners = owners;
		this.userRepository = userRepository;
	}

	private boolean isOwnerRoleUser(Authentication auth) {
		return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
	}

	private User resolveCurrentUser(Authentication auth) {
		return userRepository.findByEmail(auth.getName())
			.orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + auth.getName()));
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@ModelAttribute("owner")
	public Owner findOwner(@PathVariable(name = "ownerId", required = false) Integer ownerId) {
		return ownerId == null ? new Owner()
				: this.owners.findById(ownerId)
					.orElseThrow(() -> new ResourceNotFoundException(
							"Owner not found with id: " + ownerId + ". Please ensure the ID is correct."));
	}

	@GetMapping("/owners/new")
	public String initCreationForm() {
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/owners/new")
	public String processCreationForm(@Valid Owner owner, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in creating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		Optional<Owner> existingOwner = this.owners.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndTelephone(
				owner.getFirstName(), owner.getLastName(), owner.getTelephone());
		if (existingOwner.isPresent()) {
			result.reject("duplicate.owner",
					"An owner with this name already exists. Please search for the existing owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		try {
			this.owners.save(owner);
		}
		catch (DataIntegrityViolationException ex) {
			result.reject("duplicate.owner",
					"An owner with this name already exists. Please search for the existing owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		redirectAttributes.addFlashAttribute("message", "New Owner Created");
		return "redirect:/owners/" + owner.getId();
	}

	@GetMapping("/owners/find")
	public String initFindForm() {
		return "owners/findOwners";
	}

	@GetMapping("/owners")
	public String processFindForm(@RequestParam(defaultValue = "1") int page, Owner owner, BindingResult result,
			Model model, Authentication auth) {
		// Normalize empty strings to null so the repository treats them as "no filter"
		String lastName = nullIfEmpty(owner.getLastName());
		String telephone = nullIfEmpty(owner.getTelephone());
		String city = nullIfEmpty(owner.getCity());

		// Validate telephone format when provided (must be exactly 10 digits)
		if (telephone != null && !telephone.matches("\\d{10}")) {
			result.rejectValue("telephone", "telephone.invalid");
			return "owners/findOwners";
		}

		Page<Owner> ownersResults = findPaginatedByFilters(page, lastName, telephone, city);

		// For OWNER-role users, filter results to only show their own record
		if (isOwnerRoleUser(auth)) {
			User currentUser = resolveCurrentUser(auth);
			int linkedOwnerId = currentUser.getOwner().getId();
			List<Owner> filtered = ownersResults.getContent()
				.stream()
				.filter(o -> o.getId() != null && o.getId().equals(linkedOwnerId))
				.toList();
			if (filtered.isEmpty()) {
				result.reject("notFound", "not found");
				return "owners/findOwners";
			}
			return "redirect:/owners/" + filtered.get(0).getId();
		}

		if (ownersResults.isEmpty()) {
			result.reject("notFound", "not found");
			return "owners/findOwners";
		}

		if (ownersResults.getTotalElements() == 1) {
			owner = ownersResults.iterator().next();
			return "redirect:/owners/" + owner.getId();
		}

		return addPaginationModel(page, model, ownersResults, lastName, telephone, city);
	}

	private static String nullIfEmpty(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	@GetMapping(value = "/owners.csv", produces = "text/csv")
	public ResponseEntity<String> exportOwnersCsv(@RequestParam(defaultValue = "") String lastName) {
		List<Owner> results = owners.findByLastNameStartingWith(lastName, Pageable.unpaged()).getContent();
		StringBuilder csv = new StringBuilder("id,firstName,lastName,address,city,telephone\n");
		for (Owner owner : results) {
			csv.append('"')
				.append(owner.getId())
				.append('"')
				.append(',')
				.append(csvField(owner.getFirstName()))
				.append(',')
				.append(csvField(owner.getLastName()))
				.append(',')
				.append(csvField(owner.getAddress()))
				.append(',')
				.append(csvField(owner.getCity()))
				.append(',')
				.append(csvField(owner.getTelephone()))
				.append('\n');
		}
		return ResponseEntity.ok(csv.toString());
	}

	private static String csvField(String value) {
		if (value == null) {
			return "\"\"";
		}
		String sanitized = value.replace("\r\n", " ").replace('\r', ' ').replace('\n', ' ');
		if (!sanitized.isEmpty() && "=+-@".indexOf(sanitized.charAt(0)) >= 0) {
			sanitized = "'" + sanitized;
		}
		return "\"" + sanitized.replace("\"", "\"\"") + "\"";
	}

	private String addPaginationModel(int page, Model model, Page<Owner> paginated, String lastName, String telephone,
			String city) {
		List<Owner> listOwners = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listOwners", listOwners);
		model.addAttribute("lastName", lastName);
		model.addAttribute("telephone", telephone);
		model.addAttribute("city", city);
		return "owners/ownersList";
	}

	private Page<Owner> findPaginatedByFilters(int page, String lastName, String telephone, String city) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return owners.findByFilters(lastName, telephone, city, pageable);
	}

	@GetMapping("/owners/{ownerId}/edit")
	public String initUpdateOwnerForm(@PathVariable("ownerId") int ownerId, Authentication auth) {
		if (isOwnerRoleUser(auth)) {
			User currentUser = resolveCurrentUser(auth);
			if (!currentUser.getOwner().getId().equals(ownerId)) {
				throw new AccessDeniedException("Access denied");
			}
		}
		return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/owners/{ownerId}/edit")
	public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result, @PathVariable("ownerId") int ownerId,
			RedirectAttributes redirectAttributes, Authentication auth) {
		if (isOwnerRoleUser(auth)) {
			User currentUser = resolveCurrentUser(auth);
			if (!currentUser.getOwner().getId().equals(ownerId)) {
				throw new AccessDeniedException("Access denied");
			}
		}

		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in updating the owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		if (!Objects.equals(owner.getId(), ownerId)) {
			result.rejectValue("id", "mismatch", "The owner ID in the form does not match the URL.");
			redirectAttributes.addFlashAttribute("error", "Owner ID mismatch. Please try again.");
			return "redirect:/owners/{ownerId}/edit";
		}

		Optional<Owner> existingOwner = this.owners.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndTelephone(
				owner.getFirstName(), owner.getLastName(), owner.getTelephone());
		if (existingOwner.isPresent() && !Objects.equals(existingOwner.get().getId(), ownerId)) {
			result.reject("duplicate.owner",
					"An owner with this name already exists. Please search for the existing owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}

		owner.setId(ownerId);
		try {
			this.owners.save(owner);
		}
		catch (DataIntegrityViolationException ex) {
			result.reject("duplicate.owner",
					"An owner with this name already exists. Please search for the existing owner.");
			return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
		}
		redirectAttributes.addFlashAttribute("message", "Owner Values Updated");
		return "redirect:/owners/{ownerId}";
	}

	/**
	 * Custom handler for displaying an owner.
	 * @param ownerId the ID of the owner to display
	 * @return a ModelMap with the model attributes for the view
	 */
	@GetMapping("/owners/{ownerId}")
	public ModelAndView showOwner(@PathVariable("ownerId") int ownerId, Authentication auth) {
		ModelAndView mav = new ModelAndView("owners/ownerDetails");
		Optional<Owner> optionalOwner = this.owners.findById(ownerId);
		Owner owner = optionalOwner.orElseThrow(() -> new ResourceNotFoundException(
				"Owner not found with id: " + ownerId + ". Please ensure the ID is correct "));
		if (isOwnerRoleUser(auth)) {
			User currentUser = resolveCurrentUser(auth);
			if (!currentUser.getOwner().getId().equals(ownerId)) {
				throw new AccessDeniedException("Access denied");
			}
		}
		mav.addObject("canEdit", true);
		mav.addObject(owner);
		return mav;
	}

}
