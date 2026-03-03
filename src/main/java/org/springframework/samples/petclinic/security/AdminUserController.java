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

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.system.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Controller for admin user management at {@code /admin/users/**}.
 */
@Controller
@RequestMapping("/admin/users")
class AdminUserController {

	private final UserRepository userRepository;

	private final OwnerRepository ownerRepository;

	private final PasswordEncoder passwordEncoder;

	AdminUserController(UserRepository userRepository, OwnerRepository ownerRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.ownerRepository = ownerRepository;
		this.passwordEncoder = passwordEncoder;
	}

	// ============================================================
	// User List
	// ============================================================

	@GetMapping
	public String listUsers(Model model, Authentication auth) {
		List<User> users = userRepository.findAll();
		model.addAttribute("users", users);
		model.addAttribute("currentUserEmail", auth.getName());
		User currentUser = userRepository.findByEmail(auth.getName())
			.orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
		model.addAttribute("currentUserId", currentUser.getId());
		return "admin/userList";
	}

	// ============================================================
	// Create New Admin User
	// ============================================================

	@GetMapping("/new")
	public String showCreateAdminForm(Model model) {
		model.addAttribute("adminUserForm", new AdminUserForm());
		return "admin/createAdminUserForm";
	}

	@PostMapping("/new")
	public String processCreateAdminForm(@Valid AdminUserForm form, BindingResult result,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			return "admin/createAdminUserForm";
		}
		Optional<User> existing = userRepository.findByEmail(form.getEmail());
		if (existing.isPresent()) {
			result.rejectValue("email", "duplicate.email", "An account with this email already exists.");
			return "admin/createAdminUserForm";
		}
		User user = new User();
		user.setEmail(form.getEmail());
		user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
		user.setRole(Role.ADMIN);
		user.setOwner(null);
		userRepository.save(user);
		redirectAttributes.addFlashAttribute("message", "Admin account created successfully.");
		return "redirect:/admin/users";
	}

	// ============================================================
	// Edit User
	// ============================================================

	@GetMapping("/{id}/edit")
	public String showEditForm(@PathVariable int id, Model model, Authentication auth) {
		User currentUser = userRepository.findByEmail(auth.getName())
			.orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
		if (currentUser.getId() == id) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot edit your own account.");
		}
		User user = userRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		UserEditForm form = new UserEditForm();
		form.setId(user.getId());
		form.setEmail(user.getEmail());
		form.setRole(user.getRole());
		if (user.getOwner() != null) {
			Owner owner = user.getOwner();
			form.setFirstName(owner.getFirstName());
			form.setLastName(owner.getLastName());
			form.setAddress(owner.getAddress());
			form.setCity(owner.getCity());
			form.setTelephone(owner.getTelephone());
		}
		model.addAttribute("userEditForm", form);
		model.addAttribute("roles", Role.values());
		return "admin/editUserForm";
	}

	@PostMapping("/{id}/edit")
	public String processEditForm(@Valid UserEditForm form, BindingResult result, @PathVariable int id, Model model,
			Authentication auth, RedirectAttributes redirectAttributes) {
		User currentUser = userRepository.findByEmail(auth.getName())
			.orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
		if (currentUser.getId() == id) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot edit your own account.");
		}
		User user = userRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

		// Email uniqueness check (exclude current user)
		Optional<User> emailConflict = userRepository.findByEmail(form.getEmail());
		if (emailConflict.isPresent() && !emailConflict.get().getId().equals(id)) {
			result.rejectValue("email", "duplicate.email", "An account with this email already exists.");
		}

		if (result.hasErrors()) {
			model.addAttribute("roles", Role.values());
			return "admin/editUserForm";
		}

		user.setEmail(form.getEmail());
		if (form.getPassword() != null && !form.getPassword().isBlank()) {
			user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
		}

		Role newRole = form.getRole();
		Role existingRole = user.getRole();

		if (newRole == Role.ADMIN && existingRole == Role.OWNER) {
			user.setOwner(null);
		}
		else if (newRole == Role.OWNER && existingRole == Role.ADMIN) {
			boolean ownerFieldsValid = validateOwnerFields(form, result);
			if (!ownerFieldsValid) {
				model.addAttribute("roles", Role.values());
				return "admin/editUserForm";
			}
			Owner newOwner = new Owner();
			newOwner.setFirstName(form.getFirstName());
			newOwner.setLastName(form.getLastName());
			newOwner.setAddress(form.getAddress());
			newOwner.setCity(form.getCity());
			newOwner.setTelephone(form.getTelephone());
			ownerRepository.save(newOwner);
			user.setOwner(newOwner);
		}
		else if (newRole == Role.OWNER && user.getOwner() != null) {
			Owner existingOwner = user.getOwner();
			existingOwner.setFirstName(form.getFirstName());
			existingOwner.setLastName(form.getLastName());
			existingOwner.setAddress(form.getAddress());
			existingOwner.setCity(form.getCity());
			existingOwner.setTelephone(form.getTelephone());
			ownerRepository.save(existingOwner);
		}

		user.setRole(newRole);
		userRepository.save(user);
		redirectAttributes.addFlashAttribute("message", "User account updated successfully.");
		return "redirect:/admin/users";
	}

	private boolean validateOwnerFields(UserEditForm form, BindingResult result) {
		boolean valid = true;
		if (form.getFirstName() == null || form.getFirstName().isBlank()) {
			result.rejectValue("firstName", "required", "First name is required.");
			valid = false;
		}
		if (form.getLastName() == null || form.getLastName().isBlank()) {
			result.rejectValue("lastName", "required", "Last name is required.");
			valid = false;
		}
		if (form.getAddress() == null || form.getAddress().isBlank()) {
			result.rejectValue("address", "required", "Address is required.");
			valid = false;
		}
		if (form.getCity() == null || form.getCity().isBlank()) {
			result.rejectValue("city", "required", "City is required.");
			valid = false;
		}
		if (form.getTelephone() == null || !form.getTelephone().matches("\\d{10}")) {
			result.rejectValue("telephone", "telephone.invalid", "Telephone must be a 10-digit number.");
			valid = false;
		}
		return valid;
	}

	// ============================================================
	// Delete User
	// ============================================================

	@PostMapping("/{id}/delete")
	public String deleteUser(@PathVariable int id, @RequestParam(defaultValue = "false") boolean cascadeOwner,
			Authentication auth, RedirectAttributes redirectAttributes) {
		User currentUser = userRepository.findByEmail(auth.getName())
			.orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
		if (currentUser.getId() == id) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot delete your own account.");
		}
		User user = userRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		if (cascadeOwner && user.getOwner() != null) {
			ownerRepository.delete(user.getOwner());
		}
		userRepository.deleteById(id);
		redirectAttributes.addFlashAttribute("message", "User account deleted successfully.");
		return "redirect:/admin/users";
	}

}
