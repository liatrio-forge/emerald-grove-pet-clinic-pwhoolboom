# 14-spec-user-authentication.md

## Introduction/Overview

This feature adds a basic authentication and role-based authorization system to the Emerald Grove
Veterinary Clinic application. Pet owners can register an account (providing their email, password,
and full owner profile in one step), log in, and access only their own data. An admin role provides
unrestricted access to all owners, pets, and visits. The AI chatbot is made context-aware so that
regular users receive responses scoped to their own pets and visits, while admin users retain full
access to all clinic data.

## Goals

- Allow pet owners to self-register using an email address and full owner profile in a single step
- Protect all owner, pet, and visit pages behind authentication; unauthenticated users are
  redirected to the login page
- Enforce data isolation so that regular users can only view and edit their own owner profile,
  pets, and visits
- Provide an admin role that bypasses data isolation and can access all records across the system
- Make the AI chatbot context-aware so it scopes its responses to the authenticated user's data
  (admins receive unrestricted responses)

## User Stories

- **As a new pet owner**, I want to register an account with my email and owner details so that I
  can access my clinic records online without calling the office.
- **As a returning pet owner**, I want to log in with my email and password so that I can view and
  manage my pet information securely.
- **As a logged-in pet owner**, I want to see only my own profile, pets, and visits so that my
  private medical data is not accessible to other users.
- **As a logged-in pet owner**, I want to use the chatbot to ask about my own pets and upcoming
  visits so that I can get personalized assistance.
- **As a clinic admin**, I want to view all owners, pets, and visits across the system so that I
  can manage the clinic's complete records.
- **As a clinic admin**, I want to use the chatbot to query any owner's data so that I can assist
  clinic staff efficiently.

## Demoable Units of Work

### Unit 1: Auth Foundation — Registration, Login & Seeded Accounts

**Purpose:** Establish the complete authentication infrastructure: the `User` entity and database
schema, Spring Security configuration, registration and login pages, navbar updates, a seeded admin
account, and sample user accounts for the existing 10 sample owners.

**Functional Requirements:**

- The system shall add the `spring-boot-starter-security` dependency to `pom.xml`.
- The system shall create a `User` entity with fields: `id`, `email` (unique, not null), `passwordHash`
  (not null), `role` (enum: `OWNER` or `ADMIN`), and an optional `OneToOne` link to an `Owner`
  record (null for admin users who are not clinic owners).
- The system shall create a `users` table in the schema files for H2, MySQL, and PostgreSQL with
  columns: `id`, `email`, `password_hash`, `role`, and `owner_id` (nullable FK to `owners`).
- The system shall hash all passwords using BCrypt before storing them.
- The system shall provide a `/register` page with a form collecting: email, password, first name,
  last name, address, city, and telephone. Submitting the form creates both a `User` record
  (role=OWNER) and a linked `Owner` record.
- The system shall validate the registration form: email must be unique, password must not be blank,
  and all owner fields must satisfy the existing `Owner` validation constraints.
- The system shall provide a `/login` page using Spring Security's form login. On successful login,
  the user is redirected to their own owner detail page.
- The system shall provide a logout mechanism accessible via a POST to `/logout`, after which the
  user is redirected to the `/login` page.
- The system shall update `fragments/layout.html` so the navbar shows "Login" and "Register" links
  when the user is unauthenticated, and shows the authenticated user's email with a "Logout" button
  when logged in.
- The system shall seed one admin user account in `data.sql` for all three database profiles:
  email `admin@petclinic.com`, BCrypt-hashed password for `admin`, role `ADMIN`, no linked owner.
- The system shall seed one user account per existing sample owner in `data.sql`, using email
  pattern `<firstname>.<lastname>@petclinic.com` and BCrypt-hashed password `password`, role
  `OWNER`, linked to the corresponding owner record.
- The system shall implement a `UserDetailsService` that loads users by email for Spring Security
  authentication.

**Proof Artifacts:**

- Screenshot: `/register` page showing the full registration form with all fields demonstrates
  the registration UI exists.
- Screenshot: `/login` page demonstrates the login form is rendered.
- Screenshot: Navbar after login showing authenticated user's email and a Logout button
  demonstrates session-based authentication is working.
- Test: `UserRegistrationControllerTests` passes — verifies form validation, duplicate email
  rejection, and successful user+owner creation demonstrates the registration backend is correct.
- Test: `UserDetailsServiceTests` passes — verifies users are loaded by email and passwords are
  verified via BCrypt demonstrates the security service layer is correct.

---

### Unit 2: Owner Data Isolation for Regular Users

**Purpose:** Enforce that unauthenticated users cannot access any owner/pet/visit pages, and that
logged-in regular users can only see and edit their own owner profile, pets, and visits.

**Functional Requirements:**

- The system shall require authentication for all routes under `/owners/**`, `/pets/**`,
  `/visits/**`, and `/api/chat`. Unauthenticated requests to these routes shall be redirected
  to `/login`.
- The system shall allow unauthenticated access to `/`, `/vets.html`, and `/vets` (the public vet
  directory and home page remain public).
- The system shall redirect a regular user (role=OWNER) immediately to their own owner detail page
  (`/owners/{their-owner-id}`) after successful login.
- The system shall return a 403 Forbidden response when a regular user attempts to access an owner
  detail page (`/owners/{id}`) for an owner other than their linked owner.
- The system shall filter the "Find Owners" search results (`/owners?lastName=...`) so that a
  regular user only sees their own owner record, regardless of search input.
- The system shall allow a regular user to edit only their own owner profile and their own pets and
  visits.
- The system shall protect the Upcoming Visits page (`/visits/upcoming`) so that regular users see
  only their own pets' upcoming visits, while admin users see all upcoming visits.

**Proof Artifacts:**

- Screenshot: Browser redirect to `/login` when navigating to `/owners/1` while unauthenticated
  demonstrates unauthenticated access is blocked.
- Screenshot: "Find Owners" results page for a regular user showing only their own record
  demonstrates data isolation on the search page.
- Screenshot: 403 page when a regular user attempts to access another owner's URL directly
  demonstrates direct URL access is blocked.
- Test: `OwnerControllerTests` passing with mocked security principal — verifies 403 response
  for cross-owner access and filtered search results demonstrates access control logic is correct.

---

### Unit 3: Admin Role — Unrestricted Access

**Purpose:** Allow admin users to view and manage all owners, pets, and visits across the system
without the data isolation constraints applied to regular users.

**Functional Requirements:**

- The system shall allow a user with role `ADMIN` to access the "Find Owners" page and see all
  owners in search results.
- The system shall allow an admin to navigate to any owner's detail page (`/owners/{id}`) without
  restriction.
- The system shall allow an admin to view all pets and visits for any owner.
- The system shall allow an admin to view all upcoming visits on the Upcoming Visits page.
- The system shall allow the admin user seeded in `data.sql` (`admin@petclinic.com` / `admin`) to
  log in and access the above pages.
- The system shall not show an "Edit" button for owner profiles that the admin does not own
  (admin can view all but should not be able to edit other users' owner data unless explicitly
  permitted — editing remains restricted to the owner themselves).

**Proof Artifacts:**

- Screenshot: Admin user's "Find Owners" page showing all 10 sample owners demonstrates admin
  bypasses data isolation.
- Screenshot: Admin navigating to `/owners/1` (George Franklin) while logged in as admin demonstrates
  admin can access any owner detail page.
- Screenshot: Admin's Upcoming Visits page showing all visits across all owners demonstrates admin
  has unrestricted visit access.
- Test: `OwnerControllerTests` with admin principal — verifies no 403 is returned for any owner ID
  demonstrates admin bypass logic is correct.

---

### Unit 4: Chatbot Context Awareness

**Purpose:** Integrate the authentication context into the AI chatbot so that regular users receive
responses scoped only to their own pets and visits, while admin users retain full access to all
clinic data through the chatbot.

**Functional Requirements:**

- The system shall update `ChatTools` to read the authenticated user's role and linked owner ID
  from Spring Security's `SecurityContextHolder` inside each tool method.
- The system shall update the `getUpcomingVisits()` tool so that when called by a regular user
  (role=OWNER), it returns only upcoming visits for that user's linked owner's pets.
- The system shall update the `getUpcomingVisitsForOwner(ownerLastName)` tool so that when called
  by a regular user, it ignores the `ownerLastName` parameter and returns only that user's own
  upcoming visits (preventing querying another owner's data by last name).
- The system shall allow admin users to call all chat tools without restriction, returning all
  clinic data as before.
- The system shall update the chatbot system prompt to include the authenticated user's name and
  role so the LLM can personalize responses (e.g., "The logged-in user is George Franklin
  (role: OWNER). Only discuss information about this user's pets and visits.").
- The system shall protect the `/api/chat` endpoint so that unauthenticated requests receive a
  401 Unauthorized response (the frontend chat widget will only be shown to authenticated users).
- The system shall conditionally render the chat widget in `layout.html` only when the user is
  authenticated.

**Proof Artifacts:**

- Screenshot: Regular user chatbot response to "what are my upcoming visits?" showing only their
  own pets' visits demonstrates owner-scoped tool filtering.
- Screenshot: Admin chatbot response to "show me all upcoming visits" returning all visits
  demonstrates admin has full chatbot access.
- Test: `ChatToolsTests` — verifies `getUpcomingVisits()` returns filtered results for an OWNER
  principal and full results for an ADMIN principal demonstrates security-aware tool logic.

---

## Non-Goals (Out of Scope)

1. **Password reset / forgot password flow**: No "forgot my password" email link or self-service
   password reset is included in this feature.
2. **Email verification**: Registered accounts are immediately active; no email confirmation step
   is required.
3. **OAuth / social login**: Only email + password authentication is supported; no Google, GitHub,
   or other social providers.
4. **Multi-factor authentication (MFA)**: No TOTP, SMS, or other second factor.
5. **Admin UI for user management**: Admins cannot promote other users to admin or deactivate
   accounts through the UI. Admin accounts are created via `data.sql` seeding only.
6. **Vet user accounts**: Veterinarian records are not linked to login accounts in this feature.
7. **Admin editing of other owners' profiles**: Admins can view all owner data but cannot edit
   another user's owner profile (editing remains the owner's responsibility).
8. **Remember Me / persistent sessions**: No "remember me" checkbox or long-lived session cookies.

## Design Considerations

The existing application uses Bootstrap 5 and Thymeleaf templates. The auth UI should follow the
same visual style:

- The `/login` page should match the existing form layout style (similar card/form styling as
  `createOrUpdateOwnerForm.html`).
- The `/register` page reuses the same form field fragments (`inputField.html`) already used
  throughout the app.
- The navbar additions (Login/Register links and the email + Logout display) should fit within
  the existing Bootstrap 5 navbar structure in `fragments/layout.html`.
- Thymeleaf Security dialect (`thymeleaf-extras-springsecurity6`) should be used for conditional
  rendering of navbar elements based on authentication state (e.g.,
  `sec:authorize="isAuthenticated()"`).

## Repository Standards

- **TDD mandatory**: All new code must follow the Red-Green-Refactor cycle. Tests must be written
  before implementation.
- **Minimum 90% line coverage** on new classes; 100% branch coverage for security-critical logic
  (access control decisions).
- **@WebMvcTest** pattern for controller tests with `@MockitoBean` for dependencies (follows
  existing `OwnerControllerTests` pattern).
- **@DataJpaTest** for repository/entity tests (follows existing `ClinicServiceTests` pattern).
- **Arrange-Act-Assert** pattern with descriptive test method names.
- **Conventional commits**: Use `feat:`, `fix:`, `test:` prefixes per existing repository history.
- **Package structure**: New security classes go in a new `security` package at
  `org.springframework.samples.petclinic.security`.
- All schema changes must be applied to all three database files: `db/h2/schema.sql`,
  `db/mysql/schema.sql`, `db/postgres/schema.sql`, and their corresponding `data.sql` files.

## Technical Considerations

- **Spring Security dependency**: Add `spring-boot-starter-security` to `pom.xml` and
  `build.gradle`. Also add `thymeleaf-extras-springsecurity6` for Thymeleaf template integration.
- **User entity**: A new `User` entity in the `security` package with a `@OneToOne` optional
  relationship to `Owner`. The admin user has `owner_id = null`.
- **Role**: A Java `enum Role { OWNER, ADMIN }` stored as a `VARCHAR` column in the `users` table.
- **Password encoding**: `BCryptPasswordEncoder` bean configured in `SecurityConfig`. All passwords
  stored as BCrypt hashes. Pre-computed BCrypt hashes for sample `data.sql` passwords must be
  generated during development (they can be hardcoded as string literals in `data.sql`).
- **SecurityConfig**: A `@Configuration` class extending or annotating `SecurityFilterChain` bean,
  configuring form login (`/login`), logout (`/logout`), and the URL authorization rules
  described in Unit 2.
- **UserDetailsService**: A service implementing Spring Security's `UserDetailsService`, loading
  `User` by email and mapping `Role` to Spring Security `GrantedAuthority`.
- **ChatTools security context**: `ChatTools` is a Spring `@Component`. Tool methods can call
  `SecurityContextHolder.getContext().getAuthentication()` to determine the current user and
  their linked owner ID at method invocation time. No structural changes to `ChatService` are
  required.
- **Upcoming Visits filtering**: The `VisitRepository.findUpcomingVisits()` query may need a
  new overload `findUpcomingVisitsByOwnerId(int ownerId, ...)` to support owner-scoped filtering
  in both the `UpcomingVisitsController` and `ChatTools`.
- **CSRF**: Spring Security CSRF protection is enabled by default for all state-changing requests.
  The existing chat widget uses `POST /api/chat` — the CSRF token must be included in the
  fetch request from the chat JavaScript or the API endpoint must be configured appropriately.
- **Existing `@WebMvcTest` tests**: Adding Spring Security will cause existing controller tests
  to fail with 401/403 unless they are updated to include `@WithMockUser` or equivalent security
  context setup. All existing controller tests must be updated as part of this feature.

## Security Considerations

- Passwords must never be stored in plaintext. BCrypt hashing is required.
- The `password_hash` column must never be exposed in any API response or Thymeleaf model attribute.
- The seeded admin credentials (`admin@petclinic.com` / `admin`) are for development/demo only and
  are not suitable for production deployment — this should be documented in `DEVELOPMENT.md`.
- CSRF tokens must be included in all state-changing form submissions and AJAX requests (POST to
  `/api/chat`, POST to `/logout`, etc.).
- Direct object reference attacks (a regular user guessing another owner's `/owners/{id}` URL) must
  be blocked at the controller level, not just filtered from search results.
- The `UserRepository` (or equivalent) must use parameterized queries (Spring Data JPA handles this
  automatically) to prevent SQL injection.
- No sensitive data (BCrypt hash, role, owner_id) should be committed to version control in plain
  text beyond what is already in `data.sql` as pre-computed hashes for sample data.

## Success Metrics

1. **All existing tests pass** after adding Spring Security (updated with `@WithMockUser` as needed)
   with no regressions.
2. **New tests achieve ≥ 90% line coverage** on all new classes in the `security` package and all
   modified controller/service classes.
3. **Unauthenticated access blocked**: Navigating to `/owners/1` without logging in redirects to
   `/login`.
4. **Data isolation verified**: A regular user logged in as George Franklin cannot access
   `/owners/2` (Betty Davis) — receives a 403 response.
5. **Admin access verified**: The seeded admin user can view all 10 sample owners and all upcoming
   visits.
6. **Chatbot scoping verified**: The chatbot returns only George Franklin's pets and visits when
   queried by that logged-in user, and returns all data when queried by the admin.

## Open Questions

1. Should the CSV export endpoint (`/owners/export`) be restricted to admin users only, or should
   regular users be able to export their own owner data as a single-row CSV? (Not addressed in the
   original requirements — assume admin-only for now.)
2. Should the `/vets` REST API endpoint (used by the vet list page) require authentication, or
   remain public? (Vets are general clinic info — assume public for now.)
3. The chat widget JavaScript currently opens an SSE stream via a `POST /api/chat`. After adding
   CSRF protection, the JS must include the CSRF token in the request headers. The approach
   (meta tag in layout vs. cookie-based CSRF) should be confirmed during implementation.
