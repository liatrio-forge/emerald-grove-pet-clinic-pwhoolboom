# 14-tasks-user-authentication.md

## Relevant Files

### New Files to Create

- `src/main/java/org/springframework/samples/petclinic/security/Role.java` — Enum with values OWNER and ADMIN representing the two application roles.
- `src/main/java/org/springframework/samples/petclinic/security/User.java` — JPA entity holding email, BCrypt-hashed password, role, and optional OneToOne link to Owner.
- `src/main/java/org/springframework/samples/petclinic/security/UserRepository.java` — Spring Data JPA repository with `findByEmail(String email)`.
- `src/main/java/org/springframework/samples/petclinic/security/UserDetailsServiceImpl.java` — Spring Security `UserDetailsService` that loads a `User` by email and maps `Role` to `GrantedAuthority`.
- `src/main/java/org/springframework/samples/petclinic/security/SecurityConfig.java` — `SecurityFilterChain` bean: URL authorization rules, form login at `/login`, logout at `/logout`, CSRF config, and `BCryptPasswordEncoder` bean.
- `src/main/java/org/springframework/samples/petclinic/security/OwnerAuthenticationSuccessHandler.java` — Redirects OWNER-role users to `/owners/{owner-id}` and ADMIN-role users to `/owners/find` after login.
- `src/main/java/org/springframework/samples/petclinic/security/RegistrationForm.java` — DTO/form-backing object with fields: email, password, firstName, lastName, address, city, telephone, and validation annotations.
- `src/main/java/org/springframework/samples/petclinic/security/RegistrationService.java` — Creates both a `User` (BCrypt-hashed, role=OWNER) and a linked `Owner` in a single transaction; enforces email uniqueness.
- `src/main/java/org/springframework/samples/petclinic/security/RegistrationController.java` — Handles `GET /register` (renders form) and `POST /register` (delegates to `RegistrationService`, handles errors, redirects on success).
- `src/main/resources/templates/security/login.html` — Login page with a Bootstrap 5 card (email, password, submit, link to /register).
- `src/main/resources/templates/security/register.html` — Registration page using the existing `inputField.html` fragment for all fields.
- `src/test/java/org/springframework/samples/petclinic/security/UserRepositoryTests.java` — `@DataJpaTest` verifying User persistence and `findByEmail`.
- `src/test/java/org/springframework/samples/petclinic/security/UserDetailsServiceImplTests.java` — Unit tests for `UserDetailsServiceImpl`: happy path and `UsernameNotFoundException`.
- `src/test/java/org/springframework/samples/petclinic/security/RegistrationControllerTests.java` — `@WebMvcTest` for `RegistrationController`: GET, successful POST, duplicate-email error, blank-password error.

### Existing Files to Modify

- `pom.xml` — Add `spring-boot-starter-security` and `thymeleaf-extras-springsecurity6` dependencies.
- `build.gradle` — Mirror the same dependency additions for Gradle builds.
- `src/main/resources/db/h2/schema.sql` — Add `users` table DDL.
- `src/main/resources/db/h2/data.sql` — Add admin user INSERT and 10 sample owner user INSERTs.
- `src/main/resources/db/mysql/schema.sql` — Add `users` table DDL.
- `src/main/resources/db/mysql/data.sql` — Add admin user INSERT and 10 sample owner user INSERTs.
- `src/main/resources/db/postgres/schema.sql` — Add `users` table DDL.
- `src/main/resources/db/postgres/data.sql` — Add admin user INSERT and 10 sample owner user INSERTs.
- `src/main/resources/templates/fragments/layout.html` — Navbar auth links (Login/Register when anonymous, email+Logout when authenticated); CSRF meta tag; conditional chat widget visibility.
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — Add ownership guard to `showOwner`, `initUpdateOwnerForm`, `processUpdateOwnerForm`; filter `processFindForm` results for OWNER role; add `canEdit` model attribute.
- `src/main/java/org/springframework/samples/petclinic/owner/VisitRepository.java` — Add `findUpcomingVisitsByOwnerId(int ownerId, LocalDate start, LocalDate end)` query.
- `src/main/java/org/springframework/samples/petclinic/owner/UpcomingVisitsController.java` — Use owner-scoped query for OWNER role; unfiltered query for ADMIN.
- `src/main/resources/templates/owners/ownerDetails.html` — Wrap Edit Owner link with `th:if="${canEdit}"`.
- `src/main/java/org/springframework/samples/petclinic/chat/ChatTools.java` — Add SecurityContext-based filtering to `getUpcomingVisits()` and `getUpcomingVisitsForOwner()`.
- `src/main/java/org/springframework/samples/petclinic/chat/ChatService.java` — Include authenticated user's name and role in the system prompt; accept user context from controller.
- `src/test/java/org/springframework/samples/petclinic/owner/OwnerControllerTests.java` — Add `@WithMockUser` to existing tests; add new auth-specific tests.
- `src/test/java/org/springframework/samples/petclinic/owner/PetControllerTests.java` — Add `@WithMockUser` to restore passing state.
- `src/test/java/org/springframework/samples/petclinic/owner/VisitControllerTests.java` — Add `@WithMockUser` to restore passing state.
- `src/test/java/org/springframework/samples/petclinic/owner/UpcomingVisitsControllerTests.java` — Add `@WithMockUser`; add new role-specific tests.
- `src/test/java/org/springframework/samples/petclinic/vet/VetControllerTests.java` — Add `@WithMockUser` if VetController routes now require auth.
- `src/test/java/org/springframework/samples/petclinic/system/CrashControllerTests.java` — Add `@WithMockUser` to restore passing state.
- `src/test/java/org/springframework/samples/petclinic/chat/ChatControllerTests.java` — Add `@WithMockUser` and add unauthenticated-401 test.
- `src/test/java/org/springframework/samples/petclinic/chat/ChatToolsTests.java` — Add tests for OWNER-scoped and ADMIN-unscoped tool behavior.

### Notes

- Run `./mvnw test` after each task to verify the test suite stays green.
- BCrypt hashes for seed passwords can be generated by running a one-off Java snippet:
  `System.out.println(new BCryptPasswordEncoder().encode("password"));`
- The `sec:authorize` attribute in Thymeleaf requires the `thymeleaf-extras-springsecurity6`
  dependency and the `SpringSecurityDialect` (auto-configured by Spring Boot Security).
- When `@WebMvcTest` is combined with Spring Security, the test slice auto-configures security.
  Use `@WithMockUser` at the class level for tests that need a generic authenticated user; use
  `@WithMockUser(roles = "ADMIN")` where admin behaviour is under test.
- The `SecurityContextHolder` is thread-local. Spring AI tool invocations happen on an async
  thread during SSE streaming; if `SecurityContextHolder.getContext()` returns an empty context
  in ChatTools, add `SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)`
  to `SecurityConfig`, OR capture the `Authentication` in `ChatController` before the Flux and
  pass it through a request-scoped bean that `ChatTools` injects.
- All schema changes must be applied to all three database files (H2, MySQL, PostgreSQL).

---

## Tasks

### [x] 1.0 Auth Infrastructure — User Entity, Security Config, Login/Register & Seeded Accounts

#### 1.0 Proof Artifact(s)

- Screenshot: `/register` page showing all form fields (email, password, first/last name, address,
  city, telephone) demonstrates the registration UI is complete.
- Screenshot: `/login` page rendered at `http://localhost:8080/login` demonstrates the login form
  is served by Spring Security.
- Screenshot: Navbar after logging in as `george.franklin@petclinic.com` showing the user's email
  address and a Logout button demonstrates session-based auth is working end-to-end.
- Test: `./mvnw test` output showing all tests pass (zero failures) demonstrates Spring Security
  integration does not break the existing test suite and new auth tests pass.

#### 1.0 Tasks

- [x] 1.1 Add `spring-boot-starter-security` and `thymeleaf-extras-springsecurity6` to `pom.xml`
  (and mirror the additions in `build.gradle`); start the app with `./mvnw spring-boot:run` and
  confirm it launches without errors (Spring Security will default to a generated password in the
  logs — that is expected and will be replaced in later sub-tasks).
- [x] 1.2 Create the `security` package at
  `src/main/java/org/springframework/samples/petclinic/security/`; create `Role.java` as a Java
  `enum` with two values: `OWNER` and `ADMIN`.
- [x] 1.3 **[RED]** Write a failing `@DataJpaTest` in `UserRepositoryTests`: verify that a `User`
  can be saved with an email, BCrypt-hashed password, role, and a `null` owner link, and then
  retrieved by email via `findByEmail()`.
- [x] 1.4 Create `User.java` entity in the `security` package: `id` (auto-generated), `email`
  (`@Column(unique=true, nullable=false)`), `passwordHash` (`@Column(nullable=false)`), `role`
  (`@Enumerated(EnumType.STRING)`), and an optional `@OneToOne` to `Owner` (`@JoinColumn(name =
  "owner_id", nullable = true)`). Create `UserRepository.java` with `Optional<User>
  findByEmail(String email)`.
- [x] 1.5 Add the `users` table DDL to all three database schema files:
  - `db/h2/schema.sql`: `CREATE TABLE users (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, email VARCHAR(255) NOT NULL, password_hash VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, owner_id INTEGER, CONSTRAINT uc_user_email UNIQUE (email), CONSTRAINT fk_users_owners FOREIGN KEY (owner_id) REFERENCES owners(id));`
  - `db/mysql/schema.sql`: equivalent MySQL DDL using `AUTO_INCREMENT` and `VARCHAR`.
  - `db/postgres/schema.sql`: equivalent PostgreSQL DDL using `SERIAL` or `GENERATED ALWAYS AS IDENTITY`.
  - Run `./mvnw test -Dtest=UserRepositoryTests` — **[GREEN]** the test from step 1.3 must pass.
- [x] 1.6 Generate BCrypt hashes for the seed passwords by running this snippet in a scratch file
  or test:
  `new BCryptPasswordEncoder().encode("password")` and `new BCryptPasswordEncoder().encode("admin")`.
  Record both hash strings (they will be long strings starting with `$2a$`).
- [x] 1.7 Add seed `users` INSERT statements to all three `data.sql` files using the hashes from
  step 1.6:
  - One admin row: `email = 'admin@petclinic.com'`, hash of `'admin'`, role `'ADMIN'`, `owner_id = null`.
  - Ten owner rows, one per sample owner (owner IDs 1–10), email pattern
    `<firstname>.<lastname>@petclinic.com` (e.g., `george.franklin@petclinic.com`), hash of
    `'password'`, role `'OWNER'`, `owner_id` matching the corresponding owner's ID.
  - Note: the `users` INSERT rows must appear **after** the `owners` INSERT rows in each `data.sql`
    since they have a foreign key to `owners`.
- [x] 1.8 **[RED]** Write failing unit tests in `UserDetailsServiceImplTests`:
  `loadUserByUsername("george.franklin@petclinic.com")` returns a `UserDetails` whose username is
  the email and whose authorities contain `ROLE_OWNER`; `loadUserByUsername("nobody@example.com")`
  throws `UsernameNotFoundException`.
- [x] 1.9 Create `UserDetailsServiceImpl.java` implementing `UserDetailsService`: call
  `UserRepository.findByEmail(username)`, map the `User` to a Spring Security `User` record
  (using `User.withUsername(email).password(passwordHash).roles(role.name()).build()`), and throw
  `UsernameNotFoundException` if not found. Run `./mvnw test -Dtest=UserDetailsServiceImplTests`
  — **[GREEN]** tests must pass.
- [x] 1.10 Create `SecurityConfig.java` with:
  - A `BCryptPasswordEncoder` `@Bean`.
  - A `SecurityFilterChain` `@Bean` that:
    - Permits unauthenticated access to: `/`, `/vets`, `/vets.html`, `/login`, `/register`,
      `/webjars/**`, `/resources/**`, `/error`.
    - Requires authentication for all other routes.
    - Configures form login with `loginPage("/login")` and sets
      `OwnerAuthenticationSuccessHandler` as the success handler.
    - Configures logout: `logoutUrl("/logout")`, `logoutSuccessUrl("/login?logout")`,
      `invalidateHttpSession(true)`.
    - Keeps CSRF enabled (default); the chat widget CSRF fix is handled in Task 4.
- [x] 1.11 Create `OwnerAuthenticationSuccessHandler.java` implementing
  `AuthenticationSuccessHandler`: inject `UserRepository`; after login, look up the authenticated
  user by email; if role is `ADMIN`, redirect to `/owners/find`; if role is `OWNER`, redirect to
  `/owners/{owner.getOwner().getId()}`.
- [x] 1.12 Add `@WithMockUser` to all existing `@WebMvcTest` test classes that are now failing
  due to Spring Security auto-configuration. Apply at the class level where a generic
  authenticated user suffices: `OwnerControllerTests`, `PetControllerTests`,
  `VisitControllerTests`, `UpcomingVisitsControllerTests`, `VetControllerTests`,
  `CrashControllerTests`, `ChatControllerTests`. Run `./mvnw test` — **all pre-existing tests
  must pass before continuing**.
- [x] 1.13 **[RED]** Write failing `@WebMvcTest` tests in `RegistrationControllerTests`:
  - `GET /register` returns HTTP 200 and renders the `security/register` view.
  - `POST /register` with valid data (unique email, valid password, valid owner fields) returns a
    `3xx` redirect to `/owners/{id}`.
  - `POST /register` with a duplicate email (already in the repository) re-renders the form with
    a field or global error.
  - `POST /register` with a blank password re-renders the form with a validation error.
- [x] 1.14 Create `RegistrationForm.java` with fields `email` (`@Email @NotBlank`), `password`
  (`@NotBlank`), `firstName` (`@NotBlank`), `lastName` (`@NotBlank`), `address` (`@NotBlank`),
  `city` (`@NotBlank`), `telephone` (`@NotBlank @Pattern(regexp = "\\d{10}")`).
- [x] 1.15 Create `RegistrationService.java` with a single `@Transactional` method
  `register(RegistrationForm form)`: check `UserRepository.findByEmail(form.getEmail()).isPresent()`
  and throw a checked `DuplicateEmailException` if so; BCrypt-hash the password; create and save
  an `Owner` from the form's owner fields; create and save a `User` linked to that `Owner`;
  return the saved `User`.
- [x] 1.16 Implement `RegistrationController.java`: `GET /register` adds a new `RegistrationForm`
  to the model and returns `"security/register"`; `POST /register` calls `RegistrationService`,
  catches `DuplicateEmailException` to add a field error, and on success redirects to
  `/owners/{user.getOwner().getId()}`. Run `./mvnw test -Dtest=RegistrationControllerTests` —
  **[GREEN]** all tests from step 1.13 must pass.
- [x] 1.17 Create `src/main/resources/templates/security/register.html`: a Thymeleaf form bound
  to `registrationForm`, using the `fragments/inputField.html` fragment for each field (email,
  password, firstName, lastName, address, city, telephone), with a submit button and a link back
  to `/login`. Display global and field-level error messages.
- [x] 1.18 Create `src/main/resources/templates/security/login.html`: a standalone Thymeleaf page
  (not using layout dialect for simplicity) with a Bootstrap 5 centered card containing the Spring
  Security default form fields (`username`, `password`), a sign-in button, error message display
  (`?error`), logout success message (`?logout`), and a "Don't have an account? Register" link
  to `/register`.
- [x] 1.19 Update `src/main/resources/templates/fragments/layout.html` navbar:
  - Wrap a "Login" link (`/login`) and a "Register" link (`/register`) in
    `sec:authorize="isAnonymous()"`.
  - Wrap a display of `sec:authentication="name"` (the email) and a logout `<form method="post"
    action="/logout">` button in `sec:authorize="isAuthenticated()"`.
  - Include the Thymeleaf Security namespace: `xmlns:sec="https://www.thymeleaf.org/extras/spring-security"`.

---

### [x] 2.0 Owner Data Isolation — Protect Routes & Restrict Regular Users to Own Data

#### 2.0 Proof Artifact(s)

- Screenshot: Browser redirect to `/login` when navigating to `http://localhost:8080/owners/1`
  while not logged in demonstrates unauthenticated access is blocked.
- Screenshot: Find Owners results page while logged in as `george.franklin@petclinic.com`
  (searching with an empty last-name filter) showing only George Franklin's record demonstrates
  data isolation on the search page.
- Screenshot: Error/403 page shown when `george.franklin@petclinic.com` directly navigates to
  `/owners/2` (Betty Davis) demonstrates direct-URL access control is enforced.
- Screenshot: Upcoming Visits page for `george.franklin@petclinic.com` showing only visits for
  Leo (George's pet) demonstrates the upcoming visits page is scoped to the authenticated user.
- Test: Updated `OwnerControllerTests` passing — including new tests that verify 403 response for
  cross-owner access and filtered search results for an OWNER-role principal demonstrates access
  control logic is correct.

#### 2.0 Tasks

- [x] 2.1 **[RED]** Add new tests to `OwnerControllerTests` for OWNER-role access control (use
  `@WithMockUser(roles = "OWNER")` with a custom `Authentication` that includes a linked owner ID
  of 1, or create a `@WithUserDetails` setup using a test user seeded via `@Sql`):
  - `GET /owners/1` returns HTTP 200 for a user whose linked owner ID is 1.
  - `GET /owners/2` returns HTTP 403 for the same user.
  - `GET /owners/2/edit` returns HTTP 403 for the same user.
- [x] 2.2 Add a private helper `resolveCurrentUser(Authentication auth)` in `OwnerController`
  that looks up the `User` from `UserRepository` by email (`auth.getName()`). In `showOwner()`,
  `initUpdateOwnerForm()`, and `processUpdateOwnerForm()`: if the authenticated user's role is
  `OWNER` and their linked `owner_id` does not match the requested `ownerId`, throw
  `AccessDeniedException` (Spring Security will render a 403). ADMIN users skip this check. Run
  `./mvnw test -Dtest=OwnerControllerTests` — **[GREEN]** tests from step 2.1 must pass.
- [x] 2.3 **[RED]** Add a new test to `OwnerControllerTests`: `GET /owners` (Find Owners, empty
  filter) with an OWNER-role user (linked owner ID = 1) returns a result set containing only
  owner ID 1 (George Franklin), not all 10 owners.
- [x] 2.4 Update `OwnerController.processFindForm()`: after retrieving the `Page<Owner>` results,
  if the authenticated role is `OWNER`, filter the result to contain only the entry whose `id`
  matches the authenticated user's linked owner ID. If no entry matches (the user searched for a
  name that isn't their own), return the `findOwners` view with a "not found" result. Run
  `./mvnw test -Dtest=OwnerControllerTests` — **[GREEN]** test from step 2.3 must pass.
- [x] 2.5 **[RED]** Write a failing `@DataJpaTest` test in a new `VisitRepositoryTests` class
  (or add to `ClinicServiceTests`): `findUpcomingVisitsByOwnerId(1, today, endDate)` returns only
  the upcoming visits for owner ID 1 (Leo's visits), and not visits for owner ID 6 (Jean Coleman's
  pets — Samantha and Max).
- [x] 2.6 Add `findUpcomingVisitsByOwnerId(@Param("ownerId") int ownerId,
  @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate)` to
  `VisitRepository` with a `@Query` that mirrors `findUpcomingVisits` but adds
  `AND o.id = :ownerId` to the WHERE clause. Run the test from step 2.5 — **[GREEN]** it must
  pass.
- [x] 2.7 **[RED]** Add tests to `UpcomingVisitsControllerTests` with an OWNER-role principal
  (linked owner ID = 1): `GET /visits/upcoming` returns only Leo's visits (owner ID 1's pet),
  not visits from other owners.
- [x] 2.8 Update `UpcomingVisitsController.showUpcomingVisits()`: inject `UserRepository`; when
  the authenticated role is `OWNER`, call `findUpcomingVisitsByOwnerId()` passing the user's
  linked owner ID; when the role is `ADMIN` (or no auth check needed for admin), continue calling
  the unfiltered `findUpcomingVisits()`. Run `./mvnw test -Dtest=UpcomingVisitsControllerTests`
  — **[GREEN]** tests from step 2.7 must pass.

---

### [x] 3.0 Admin Role — Unrestricted Access to All Owners, Pets & Visits

#### 3.0 Proof Artifact(s)

- Screenshot: Find Owners page (empty last-name search) while logged in as
  `admin@petclinic.com` showing all 10 sample owners in results demonstrates admin bypasses
  data isolation.
- Screenshot: Admin navigating to `/owners/1` (George Franklin's profile) while logged in as
  admin demonstrates admin can access any owner's detail page without a 403.
- Screenshot: Upcoming Visits page while logged in as admin showing visits across multiple owners
  (both Leo's visit and Samantha/Max's visits for Jean Coleman) demonstrates admin has
  unrestricted visit access.
- Screenshot: Owner detail page viewed by admin showing the Edit button is absent (the admin does
  not own this profile) demonstrates the edit restriction is correctly enforced.
- Test: New `OwnerControllerTests` tests with an ADMIN-role principal verifying no 403 is
  returned for any owner ID and all owners appear in unfiltered search results demonstrates the
  admin bypass is correctly implemented.

#### 3.0 Tasks

- [x] 3.1 **[RED]** Add `@WithMockUser(roles = "ADMIN")` tests to `OwnerControllerTests`:
  - `GET /owners/1` returns HTTP 200.
  - `GET /owners/2` returns HTTP 200 (no 403).
  - `GET /owners/5` returns HTTP 200 (no 403).
  - `GET /owners` (Find Owners, empty filter) returns all 10 owners in the result list.
- [x] 3.2 Run `./mvnw test -Dtest=OwnerControllerTests` to check whether the ADMIN bypass logic
  written in Task 2 already satisfies the tests from step 3.1. If any tests fail, correct the
  guard logic in `OwnerController` (the `resolveCurrentUser` helper) to explicitly skip
  restrictions when `auth.getAuthorities()` contains `ROLE_ADMIN`. **[GREEN]** all tests must
  pass.
- [x] 3.3 **[RED]** Add an `@WithMockUser(roles = "ADMIN")` test to
  `UpcomingVisitsControllerTests`: `GET /visits/upcoming` returns visits for at least two
  different owners (confirming the unfiltered query is used for admins).
- [x] 3.4 Run `./mvnw test -Dtest=UpcomingVisitsControllerTests` to confirm the test from step
  3.3 passes with the logic already written in Task 2.8. If it fails, add an explicit `isAdmin`
  check in `UpcomingVisitsController` to call the unfiltered `findUpcomingVisits()` for ADMIN
  users. **[GREEN]** all tests must pass.
- [x] 3.5 In `OwnerController.showOwner()`, add a `canEdit` boolean model attribute: set to
  `true` when the authenticated user's role is `OWNER` and their linked owner ID matches the
  displayed `ownerId`; set to `false` in all other cases (including when the viewer is ADMIN).
- [x] 3.6 **[RED]** Add a test to `OwnerControllerTests` with an ADMIN principal: `GET /owners/1`
  returns a model attribute `canEdit` equal to `false`.
- [x] 3.7 Update `src/main/resources/templates/owners/ownerDetails.html`: wrap the "Edit Owner"
  button/link in `th:if="${canEdit}"` so it is only rendered when the authenticated user is the
  profile's owner. Run `./mvnw test -Dtest=OwnerControllerTests` — **[GREEN]** all tests
  including the `canEdit` assertion must pass.

---

### [x] 4.0 Chatbot Security Integration — Context-Aware Tools, CSRF & Conditional Widget

#### 4.0 Proof Artifact(s)

- Screenshot: Regular user (`george.franklin@petclinic.com`) chatbot response to the question
  "What are my upcoming visits?" showing only visits for Leo (George's pet, owner ID 1)
  demonstrates the chatbot tool is filtering results to the authenticated owner.
- Screenshot: Admin user chatbot response to "Show me all upcoming visits" returning visits for
  both Leo (owner 1) and Samantha/Max (owner 6) demonstrates the admin account has unrestricted
  chatbot access.
- Screenshot: Home page viewed while logged out showing the chat widget button is absent
  demonstrates the widget is only rendered for authenticated users.
- Test: `ChatToolsTests` passing — including tests that verify `getUpcomingVisits()` returns
  filtered results for an OWNER-role security context and full results for an ADMIN-role context
  demonstrates the security-aware tool logic is correct.

#### 4.0 Tasks

- [x] 4.1 **[RED]** Add tests to `ChatToolsTests` for the OWNER-scoped and ADMIN-unscoped
  behaviour of `getUpcomingVisits()` and `getUpcomingVisitsForOwner()`:
  - `getUpcomingVisits()` with an OWNER security context (linked owner ID = 1) returns only
    Leo's visit (owner 1), not Samantha's or Max's visits (owner 6).
  - `getUpcomingVisits()` with an ADMIN security context returns all upcoming visits.
  - `getUpcomingVisitsForOwner("Coleman")` with an OWNER security context (linked owner ID = 1)
    returns only Leo's visit (ignores the "Coleman" last name filter).
  - `getUpcomingVisitsForOwner("Coleman")` with an ADMIN security context returns Samantha's and
    Max's visits (owner 6, Jean Coleman).
  Use `SecurityContextHolder.setContext(...)` with a mock `Authentication` in each test's Arrange
  phase; clear the context in `@AfterEach`.
- [x] 4.2 Inject `UserRepository` into `ChatTools`. In `getUpcomingVisits()`: call
  `SecurityContextHolder.getContext().getAuthentication()`; if the role is `OWNER`, retrieve the
  user's linked owner ID and call `visitRepository.findUpcomingVisitsByOwnerId(ownerId, ...)` for
  filtering; if the role is `ADMIN`, continue using the unfiltered `findUpcomingVisits()`.
  Apply the same role-check in `getUpcomingVisitsForOwner()`: for `OWNER`, ignore the
  `ownerLastName` parameter and return only the authenticated user's own visits.
  Run `./mvnw test -Dtest=ChatToolsTests` — **[GREEN]** all tests from step 4.1 must pass.
- [x] 4.3 Verify that `SecurityContextHolder` is populated when `ChatTools` tool methods are
  invoked during SSE streaming. Start the app, log in as a sample owner, and ask the chatbot "What
  are my upcoming visits?". If the context is empty (tool returns no data or throws a null
  pointer), add `SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)`
  as the first line of a `@PostConstruct` method in `SecurityConfig` to propagate the context to
  child threads used by the reactive scheduler.
- [x] 4.4 Update `ChatService` to accept the authenticated user's display name and role and
  incorporate them into the system prompt. Modify the `chat()` method signature to accept
  `String userDisplayName` and `String userRole`; prepend a sentence to `SYSTEM_PROMPT` such as:
  `"The current user is [userDisplayName] (role: [userRole]). Only provide information relevant
  to this user unless the role is ADMIN."`. Update `ChatController` to resolve the authenticated
  user via `UserRepository` before calling `chatService.chat()` and pass the owner's full name
  and role string.
- [x] 4.5 **[RED]** Add a test to `ChatControllerTests` with no authentication: `POST /api/chat`
  returns HTTP 401 Unauthorized (the endpoint requires authentication per the `SecurityConfig`
  from Task 1.10).
- [x] 4.6 Run `./mvnw test -Dtest=ChatControllerTests` — the test from step 4.5 should
  **[GREEN]** pass because `SecurityConfig` already requires auth for `/api/chat`. If it fails,
  verify the URL pattern in `SecurityConfig` covers `/api/**` or explicitly `/api/chat`.
- [x] 4.7 Add a `<meta name="_csrf" th:content="${_csrf.token}"/>` and
  `<meta name="_csrf_header" th:content="${_csrf.headerName}"/>` pair to the `<head>` section of
  `src/main/resources/templates/fragments/layout.html` so JavaScript can read the CSRF token.
- [x] 4.8 Update the chat widget JavaScript in `layout.html`: before the `fetch` call to
  `POST /api/chat`, read the CSRF token and header name from the meta tags and add them to the
  request headers (e.g.,
  `headers: { [document.querySelector('meta[name="_csrf_header"]').content]: document.querySelector('meta[name="_csrf"]').content }`).
  Verify the chat widget still works end-to-end after login by starting the app and sending a
  message.
- [x] 4.9 Update `src/main/resources/templates/fragments/layout.html` to wrap the entire chat
  widget `<div>` (the floating button and the chat panel) in
  `th:if="${#authorization.expression('isAuthenticated()')}"` (or equivalently
  `sec:authorize="isAuthenticated()"`) so the widget is not rendered for unauthenticated users.
  Verify by visiting the home page while logged out — the chat bubble must not appear.
