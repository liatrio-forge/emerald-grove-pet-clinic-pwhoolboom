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

import java.time.LocalDate;

import org.springframework.samples.petclinic.security.User;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller for the upcoming visits page at {@code /visits/upcoming}.
 */
@Controller
class UpcomingVisitsController {

	private final VisitRepository visits;

	private final UserRepository userRepository;

	UpcomingVisitsController(VisitRepository visits, UserRepository userRepository) {
		this.visits = visits;
		this.userRepository = userRepository;
	}

	@GetMapping("/visits/upcoming")
	public String showUpcomingVisits(@RequestParam(defaultValue = "7") int days, Model model, Authentication auth) {
		if (days < 1 || days > 365) {
			model.addAttribute("errorMessage", "upcomingVisits.daysError");
			return "visits/upcomingVisits";
		}
		LocalDate today = LocalDate.now();
		LocalDate endDate = today.plusDays(days - 1);
		if (isOwnerRoleUser(auth)) {
			User currentUser = userRepository.findByEmail(auth.getName())
				.orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
			model.addAttribute("upcomingVisits",
					this.visits.findUpcomingVisitsByOwnerId(currentUser.getOwner().getId(), today, endDate));
		}
		else {
			model.addAttribute("upcomingVisits", this.visits.findUpcomingVisits(today, endDate));
		}
		model.addAttribute("days", days);
		return "visits/upcomingVisits";
	}

	private boolean isOwnerRoleUser(Authentication auth) {
		return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
	}

}
