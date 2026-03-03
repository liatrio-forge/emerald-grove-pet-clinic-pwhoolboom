# 15 - Admin User Management Tasks

## Relevant Files

### New Files

- `src/main/java/org/springframework/samples/petclinic/security/AdminUserController.java` — New controller handling all `/admin/users/**` routes (list, create, edit, delete).
- `src/main/java/org/springframework/samples/petclinic/security/AdminUserForm.java` — Form-backing DTO for creating a new ADMIN-role user (email + password).
- `src/main/java/org/springframework/samples/petclinic/security/UserEditForm.java` — Form-backing DTO for editing an existing user (email, optional password, role, owner fields).
- `src/main/resources/templates/admin/userList.html` — User list page showing all accounts with email, role, owner columns, and action buttons.
- `src/main/resources/templates/admin/createAdminUserForm.html` — Create new admin user form (email + password).
- `src/main/resources/templates/admin/editUserForm.html` — Edit user form with conditional owner fields and a role-change JS toggle.
- `src/test/java/org/springframework/samples/petclinic/security/AdminUserControllerTests.java` — `@WebMvcTest` tests for every endpoint in `AdminUserController`.
- `docs/specs/15-spec-admin-user-management/15-proofs/` — Directory for proof artifact screenshots.

### Modified Files

- `src/main/java/org/springframework/samples/petclinic/security/SecurityConfig.java` — Add `.requestMatchers("/admin/**").hasRole("ADMIN")` rule before `anyRequest().authenticated()`.
- `src/main/resources/templates/fragments/layout.html` — Add "Manage Users" nav link inside an `sec:authorize="hasRole('ADMIN')"` block.
- `src/main/resources/messages/messages.properties` — New i18n keys for user management UI strings.
- `src/main/resources/messages/messages_en.properties` — Same new keys in English.
- `src/main/resources/messages/messages_de.properties` — Same new keys in German.
- `src/main/resources/messages/messages_es.properties` — Same new keys in Spanish.
- `src/main/resources/messages/messages_fa.properties` — Same new keys in Farsi.
- `src/main/resources/messages/messages_ko.properties` — Same new keys in Korean.
- `src/main/resources/messages/messages_pt.properties` — Same new keys in Portuguese.
- `src/main/resources/messages/messages_ru.properties` — Same new keys in Russian.
- `src/main/resources/messages/messages_tr.properties` — Same new keys in Turkish.

### Reference Files (read for patterns, do not modify)

- `src/main/java/org/springframework/samples/petclinic/security/RegistrationController.java` — Follow this controller pattern for form binding and error handling.
- `src/main/java/org/springframework/samples/petclinic/security/RegistrationService.java` — Follow this service pattern for user creation logic.
- `src/main/java/org/springframework/samples/petclinic/security/User.java` — User entity fields and Role enum.
- `src/main/java/org/springframework/samples/petclinic/security/UserRepository.java` — Available query methods (use `findAll()` for list, `findByEmail()` for uniqueness checks).
- `src/main/java/org/springframework/samples/petclinic/owner/OwnerController.java` — Follow this pattern for direct repository injection and role-based access checks.
- `src/main/java/org/springframework/samples/petclinic/owner/Owner.java` — Check `@OneToMany` cascade settings on the `pets` field before implementing cascade delete.
- `src/main/resources/templates/owners/ownersList.html` — Copy `liatrio-table` / pagination markup style for the user list.
- `src/main/resources/templates/owners/ownerDetails.html` — Copy Bootstrap modal and flash message patterns for the delete modal.
- `src/test/java/org/springframework/samples/petclinic/security/RegistrationControllerTests.java` — Copy test imports, mock setup, and `.with(csrf())` patterns.
- `src/test/java/org/springframework/samples/petclinic/security/WebMvcTestSecurityConfig.java` — Must be imported in `AdminUserControllerTests` via `@Import`.

### Notes

- Follow **Strict TDD**: every sub-task marked `[TEST]` must be written and confirmed failing before the corresponding `[IMPL]` sub-task is started.
- Run `./mvnw spring-javaformat:apply` after every Java file change to keep the formatter happy.
- Run `./mvnw test -Dtest=AdminUserControllerTests` after each task to verify progress.
- All POST form templates must use `th:action` (not `action`) so Thymeleaf injects the CSRF hidden field automatically.
- Password values must never appear in a `th:value` attribute — the password field on the edit form must always render blank.

---

## Tasks

### [x] 1.0 Security Config, Navigation, and User List

Add the ADMIN-only route restriction to `SecurityConfig`, add a "Manage Users" nav link visible only to admin users, add all i18n keys for the new section, and implement the `GET /admin/users` endpoint and template that lists all user accounts.

#### 1.0 Proof Artifact(s)

- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-nav-admin-visible.png` — "Manage Users" nav link visible in the top bar when logged in as admin
- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-nav-owner-hidden.png` — nav bar when logged in as an owner shows no "Manage Users" link
- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-user-list.png` — `/admin/users` page showing the full user list with email, role, and owner columns
- Test: `AdminUserControllerTests` — `GET /admin/users` returns 200 for ADMIN and 403 for OWNER and unauthenticated demonstrates access control

#### 1.0 Tasks

- [x] 1.1 `[TEST]` Create `AdminUserControllerTests.java` in the `security` test package. Add the class-level annotations (`@WebMvcTest(AdminUserController.class)`, `@DisabledInNativeImage`, `@DisabledInAotMode`, `@WithMockUser(username = "admin@petclinic.com", roles = "ADMIN")`, `@Import(WebMvcTestSecurityConfig.class)`). Add `@MockitoBean` fields for `UserRepository` and `OwnerAuthenticationSuccessHandler`. Write a `@BeforeEach` that stubs `userRepository.findAll()` to return a small list of two test users (one ADMIN, one OWNER with a linked owner). Write three failing tests: `testGetUserList_asAdmin_returns200()`, `testGetUserList_asOwner_returns403()` (annotated `@WithMockUser(roles = "OWNER")`), and `testGetUserList_unauthenticated_redirectsToLogin()` (annotated `@WithAnonymousUser`). Confirm all three tests fail (controller does not exist yet).
- [x] 1.2 `[IMPL]` Open `SecurityConfig.java`. Inside the `authorizeHttpRequests` chain, add `.requestMatchers("/admin/**").hasRole("ADMIN")` as a new rule **before** the existing `.anyRequest().authenticated()` line. Run `./mvnw spring-javaformat:apply`.
- [x] 1.3 `[IMPL]` Create `AdminUserController.java` in the `security` package. Annotate with `@Controller` and `@RequestMapping("/admin/users")`. Inject `UserRepository` and `PasswordEncoder` via constructor. Add a `GET /admin/users` handler method: call `userRepository.findAll()`, add the result to the model under the key `"users"`, add the currently authenticated user's email (from `Authentication`) to the model under `"currentUserEmail"`, and return the view name `"admin/userList"`. Run `./mvnw spring-javaformat:apply`.
- [x] 1.4 `[IMPL]` Create `src/main/resources/templates/admin/userList.html`. Use `th:replace="~{fragments/layout :: layout (~{::body},'admin')}"` to wrap the page. In the body, render an `<h2>` title (using the i18n key added in 1.5), a success/error flash alert block (follow the `ownerDetails.html` pattern), and a `<table class="table table-striped liatrio-table">` with columns: Email, Role, Linked Owner, and Actions. Use `th:each="user : ${users}"` to render rows. In the Owner column, use `th:text="${user.owner != null ? user.owner.firstName + ' ' + user.owner.lastName : '—'}"`. In the Actions column, add an "Edit" link pointing to `/admin/users/{id}/edit` (to be wired up in Task 3). Leave the delete button placeholder as a comment for now — it will be added in Task 4.
- [x] 1.5 `[IMPL]` Add the following i18n keys to **all nine** message property files (`messages.properties` and all eight locale variants). For non-English files, use the English value as a placeholder — a native speaker would update them later. Keys to add: `manageUsers=Manage Users`, `users.list.title=User Management`, `users.email=Email`, `users.role=Role`, `users.linkedOwner=Linked Owner`, `users.actions=Actions`.
- [x] 1.6 `[IMPL]` Open `fragments/layout.html`. Find the `<ul>` block that contains the existing nav menu items. Add a new `<li>` element inside a `<div sec:authorize="hasRole('ADMIN')">` wrapper, using the same `th:replace="~{::menuItem ...}"` pattern as the existing items. Use the Font Awesome `users` icon, link to `/admin/users`, and the i18n key `#{manageUsers}` for the text.
- [x] 1.7 `[VERIFY]` Run `./mvnw spring-javaformat:apply` then `./mvnw test -Dtest=AdminUserControllerTests`. Confirm all three tests from step 1.1 now pass.
- [x] 1.8 `[PROOF]` Create the `docs/specs/15-spec-admin-user-management/15-proofs/` directory. Start the application (`./mvnw spring-boot:run`), log in as `admin@petclinic.com` / `admin`, take a screenshot of the nav bar showing "Manage Users" (save as `15-nav-admin-visible.png`), take a screenshot of `/admin/users` (save as `15-user-list.png`). Log out, log in as `george.franklin@petclinic.com` / `password`, take a screenshot of the nav bar showing no "Manage Users" link (save as `15-nav-owner-hidden.png`).

---

### [x] 2.0 Create New Admin User

Implement `GET /admin/users/new` (the creation form with email and password fields) and `POST /admin/users/new` (saves a new ADMIN-role user with no linked owner). Includes duplicate-email validation and a success redirect to the user list.

#### 2.0 Proof Artifact(s)

- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-create-admin-form.png` — `/admin/users/new` form rendered with email and password fields
- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-create-admin-success.png` — after submission the user list page shows a success flash message and the new admin account appears in the table
- Test: `AdminUserControllerTests` — POST with valid data redirects to `/admin/users`, POST with duplicate email re-renders form with error, POST with blank password re-renders form with error — all pass demonstrates end-to-end create flow

#### 2.0 Tasks

- [x] 2.1 `[TEST]` In `AdminUserControllerTests.java`, add a `@BeforeEach` stub so `userRepository.findByEmail(anyString())` returns `Optional.empty()` by default (no duplicates). Add four failing tests: `testGetNewAdminForm_returns200()` (expects 200 and view `"admin/createAdminUserForm"` and model attribute `"adminUserForm"`); `testPostNewAdmin_validData_redirectsToList()` (POST with valid email and password, expects 3xx redirect to `/admin/users`); `testPostNewAdmin_duplicateEmail_showsFormError()` (stub `findByEmail` to return a user, POST valid data, expect 200 and `model().attributeHasErrors("adminUserForm")`); `testPostNewAdmin_blankPassword_showsFormError()` (POST with blank password, expect 200 and `model().attributeHasFieldErrors("adminUserForm", "password")`). Confirm all four tests fail.
- [x] 2.2 `[IMPL]` Create `AdminUserForm.java` in the `security` package. It is a plain Java class (not an entity) with two fields: `String email` (annotated `@Email @NotBlank`) and `String password` (annotated `@NotBlank`). Add standard getters and setters.
- [x] 2.3 `[IMPL]` In `AdminUserController.java`, add `GET /admin/users/new`: put a `new AdminUserForm()` in the model under `"adminUserForm"` and return view `"admin/createAdminUserForm"`.
- [x] 2.4 `[IMPL]` In `AdminUserController.java`, add `POST /admin/users/new` with parameters `@Valid @ModelAttribute("adminUserForm") AdminUserForm form`, `BindingResult result`, and `RedirectAttributes redirectAttributes`. If `result.hasErrors()`, return the form view. Check for duplicate email using `userRepository.findByEmail(form.getEmail())`; if present, call `result.rejectValue("email", "duplicate.email", "An account with this email already exists.")` and return the form view. Otherwise: create a new `User`, set email, BCrypt-encode the password using the injected `PasswordEncoder`, set role to `Role.ADMIN`, set owner to `null`, save via `userRepository.save()`, add a flash message, and redirect to `/admin/users`. Run `./mvnw spring-javaformat:apply`.
- [x] 2.5 `[IMPL]` Create `src/main/resources/templates/admin/createAdminUserForm.html`. Use the layout fragment wrapper. Render an `<h2>` (i18n key from 2.6), a form with `th:action="@{/admin/users/new}"` and `method="post"`, an email input bound to `adminUserForm.email` using the `fragments/inputField` fragment, a password input (type `password`, `name="password"`, **no `th:value`**) with inline error display, and a submit button.
- [x] 2.6 `[IMPL]` Add new i18n keys to all nine message files: `createAdminUser=Create Admin User`, `users.new.title=New Admin Account`, `users.create.success=Admin account created successfully.`, `users.email.duplicate=An account with this email already exists.`
- [x] 2.7 `[VERIFY]` Run `./mvnw spring-javaformat:apply` then `./mvnw test -Dtest=AdminUserControllerTests`. Confirm all tests from tasks 1 and 2 pass.
- [x] 2.8 `[PROOF]` With the app running, navigate to `/admin/users/new` as admin, take a screenshot of the blank form (`15-create-admin-form.png`). Fill in a new email and password, submit, and take a screenshot of the user list showing the success flash message and the new account in the table (`15-create-admin-success.png`).

---

### [x] 3.0 Edit User (All Fields, Role Change, Self-Edit Block)

Implement `GET /admin/users/{id}/edit` and `POST /admin/users/{id}/edit`. The form shows email, an optional password field, a role selector, and conditionally shows owner fields (first name, last name, address, city, telephone) when the OWNER role is selected. Handles OWNER→ADMIN unlinking and ADMIN→OWNER owner creation. Hard-blocks an admin from editing their own account.

#### 3.0 Proof Artifact(s)

- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-edit-owner-user-form.png` — edit form for an OWNER-role user with all fields (email, role selector, owner details) pre-populated
- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-role-change-owner-to-admin.png` — user list after changing an OWNER-role user to ADMIN shows role as "ADMIN" and owner column as "—"
- Test: `AdminUserControllerTests` — POST to edit own account returns 403, blank password on edit leaves `passwordHash` unchanged, OWNER→ADMIN sets owner to null — all pass demonstrates edit logic

#### 3.0 Tasks

- [x] 3.1 `[TEST]` In `AdminUserControllerTests.java`, add setup that creates a test OWNER-role user (`testUser`) with id=2, email `test@example.com`, a linked owner, and stubs `userRepository.findById(2)` to return it. Also stub `userRepository.findByEmail("admin@petclinic.com")` to return a user with id=1 (the current admin). Add the following failing tests:
  - `testGetEditForm_asAdmin_returns200()` — GET `/admin/users/2/edit`, expect 200 and model attribute `"userEditForm"`.
  - `testGetEditForm_ownAccount_returns403()` — GET `/admin/users/1/edit` (own id), expect 403.
  - `testPostEdit_validData_redirectsToList()` — POST `/admin/users/2/edit` with valid email, blank password, same role; expect redirect to `/admin/users`.
  - `testPostEdit_blankPassword_preservesPasswordHash()` — POST with blank password; verify `userRepository.save()` is called with the original `passwordHash` unchanged (use `ArgumentCaptor`).
  - `testPostEdit_ownerToAdmin_unlinksOwner()` — POST changing role to ADMIN; verify the saved user has `owner == null`.
  - `testPostEdit_ownAccount_returns403()` — POST to own id; expect 403.
  - `testPostEdit_duplicateEmail_showsFormError()` — POST with an email that belongs to another user; expect 200 and form errors.
  Confirm all tests fail.
- [x] 3.2 `[IMPL]` Create `UserEditForm.java` in the `security` package. Fields: `Integer id`, `String email` (`@Email @NotBlank`), `String password` (no constraint — optional), `Role role` (`@NotNull`), `String firstName`, `String lastName`, `String address`, `String city`, `String telephone`. Add standard getters and setters. Note: owner field validation (not blank, telephone format) will be enforced in the controller when role is OWNER, not via annotations, to keep the DTO flexible.
- [x] 3.3 `[IMPL]` In `AdminUserController.java`, inject `OwnerRepository` via the constructor (needed for saving owner records on ADMIN→OWNER changes). Add `GET /admin/users/{id}/edit`: look up the user by id (throw `ResourceNotFoundException` if absent), compare the user's id against the authenticated user's id (fetched via `userRepository.findByEmail(auth.getName())`); if they match, return 403. Populate a `UserEditForm` from the user's fields (and owner fields if role is OWNER), add to model, add `Role.values()` to the model under `"roles"`, and return view `"admin/editUserForm"`. Run `./mvnw spring-javaformat:apply`.
- [x] 3.4 `[IMPL]` In `AdminUserController.java`, add `POST /admin/users/{id}/edit` with `@Valid @ModelAttribute("userEditForm") UserEditForm form`, `BindingResult result`, `@PathVariable int id`, `Authentication auth`, `RedirectAttributes redirectAttributes`. Implement in this order:
  1. Self-edit check: if `id` matches the authenticated user's id, return 403.
  2. Load the existing user from `userRepository.findById(id)`.
  3. Validate email uniqueness: if `findByEmail(form.getEmail())` returns a user whose id differs from `id`, reject the email field.
  4. Return form view if `result.hasErrors()`.
  5. Update email on the user.
  6. If `form.getPassword()` is non-blank, BCrypt-encode and set `passwordHash`; otherwise leave it unchanged.
  7. Handle role change:
     - If new role is ADMIN and existing role is OWNER: set `user.setOwner(null)`.
     - If new role is OWNER and existing role is ADMIN: validate that owner fields (firstName, lastName, address, city, telephone) are non-blank and telephone matches `\d{10}`; if validation fails, add errors to `BindingResult` and return form view; otherwise create a new `Owner`, populate its fields, save via `ownerRepository.save(owner)`, and set `user.setOwner(owner)`.
     - If role is unchanged and OWNER: update the linked owner's fields via the existing owner object and save.
  8. Set `user.setRole(form.getRole())`, save via `userRepository.save(user)`, add flash message, redirect to `/admin/users`.
  Run `./mvnw spring-javaformat:apply`.
- [x] 3.5 `[IMPL]` Create `src/main/resources/templates/admin/editUserForm.html`. Include the layout fragment wrapper. Render a form with `th:action="@{/admin/users/__${userEditForm.id}__/edit}"` and `method="post"`. Include: hidden `id` field bound to `userEditForm.id`; email input using `inputField` fragment; password input (type `password`, no `th:value`, with a hint "Leave blank to keep current password"); a `<select>` for `role` bound to `userEditForm.role` listing `Role.OWNER` and `Role.ADMIN` options. Below the role selector, add an owner fields section (`<div id="ownerFields">`) with inputs for firstName, lastName, address, city, telephone — each bound to the matching `userEditForm` property. Add a `<script>` block that listens for `change` events on the role `<select>` and toggles the visibility of `#ownerFields`: show when OWNER is selected, hide when ADMIN is selected. Set the initial visibility based on the pre-selected role value.
- [x] 3.6 `[IMPL]` Add new i18n keys to all nine message files: `editUser=Edit User`, `users.edit.title=Edit User Account`, `users.edit.success=User account updated successfully.`, `users.edit.selfEdit.error=You cannot edit your own account.`, `users.password.hint=Leave blank to keep current password.`
- [x] 3.7 `[VERIFY]` Run `./mvnw spring-javaformat:apply` then `./mvnw test -Dtest=AdminUserControllerTests`. Confirm all tests from tasks 1, 2, and 3 pass.
- [x] 3.8 `[PROOF]` With the app running, navigate to the edit page for an OWNER-role user (e.g., `/admin/users/2/edit`), take a screenshot showing the form with all owner fields populated (`15-edit-owner-user-form.png`). Change the role to ADMIN and submit, then take a screenshot of the user list showing the updated role and "—" in the owner column (`15-role-change-owner-to-admin.png`).

---

### [x] 4.0 Delete User (with Optional Owner Cascade)

Add a delete button to each row of the user list (hidden for the current admin's own row). Implement a Bootstrap confirmation modal with an "Also delete the linked owner record" checkbox for OWNER-role users. Implement `POST /admin/users/{id}/delete` for both cascade and non-cascade deletion. Hard-blocks self-deletion.

#### 4.0 Proof Artifact(s)

- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-delete-modal-cascade.png` — delete confirmation modal for an OWNER-role user showing the "Also delete the linked owner record (and all their pets and visits)" checkbox
- Screenshot: `docs/specs/15-spec-admin-user-management/15-proofs/15-owner-preserved-after-user-delete.png` — after deleting a user without the cascade option, the linked owner still appears on the Find Owners page
- Test: `AdminUserControllerTests` — POST delete of own account returns 403, POST delete with `cascadeOwner=true` removes the owner record, POST delete without cascade leaves owner intact — all pass demonstrates delete logic

#### 4.0 Tasks

- [x] 4.1 `[TEST]` In `AdminUserControllerTests.java`, using the same `testUser` stub from task 3, add the following failing tests:
  - `testPostDelete_asAdmin_redirectsToList()` — POST `/admin/users/2/delete` with no extra params; expect redirect to `/admin/users`.
  - `testPostDelete_ownAccount_returns403()` — POST to own id (1); expect 403.
  - `testPostDelete_withoutCascade_preservesOwner()` — POST with `cascadeOwner=false`; verify `ownerRepository.delete(...)` is **never** called and `userRepository.deleteById(2)` is called.
  - `testPostDelete_withCascade_deletesOwner()` — POST with `cascadeOwner=true` where the user has a linked owner; verify `ownerRepository.delete(owner)` is called and then `userRepository.deleteById(2)` is called.
  Confirm all four tests fail.
- [x] 4.2 `[IMPL]` In `AdminUserController.java`, add `POST /admin/users/{id}/delete` with parameters `@PathVariable int id`, `@RequestParam(defaultValue = "false") boolean cascadeOwner`, `Authentication auth`, `RedirectAttributes redirectAttributes`:
  1. Self-delete check: if `id` matches the authenticated user's id, return 403.
  2. Load the user via `userRepository.findById(id)` (throw `ResourceNotFoundException` if absent).
  3. If `cascadeOwner` is `true` and `user.getOwner() != null`: call `ownerRepository.delete(user.getOwner())` (JPA cascade will remove pets and their visits).
  4. Call `userRepository.deleteById(id)`.
  5. Add flash message and redirect to `/admin/users`.
  Before implementing, open `Owner.java` and verify the `pets` field has `CascadeType.ALL` or `CascadeType.REMOVE`. If not, add it before proceeding.
  Run `./mvnw spring-javaformat:apply`.
- [x] 4.3 `[IMPL]` Open `admin/userList.html`. In the controller, add `currentUserId` to the model (fetched from `userRepository.findByEmail(auth.getName()).getId()`). In the template's Actions column, add:
  - An "Edit" link (already added in 1.4 — confirm it is there).
  - A "Delete" button wrapped in `th:if="${user.id != currentUserId}"`. Use `data-bs-toggle="modal"` and `data-bs-target="#deleteUserModal"` to trigger a modal. Store the target user id, whether the user has a linked owner, and the user's name on the button using `data-*` attributes.
  At the bottom of the body, add a Bootstrap modal (`#deleteUserModal`) with:
  - A title (i18n key `deleteUser`).
  - A body paragraph showing the target user's email (populated by JavaScript from the button's `data-*` attributes).
  - A checkbox input `name="cascadeOwner"` value="true" with label from i18n key `users.delete.cascade.label`, wrapped in `th:if` so it only renders when the target user has a linked owner (controlled via JS toggle on modal show).
  - A `<form id="deleteUserForm" method="post">` with `th:action` pointing to a placeholder (updated by JavaScript when the modal opens) and an inline CSRF hidden field using `th:if="${_csrf != null}"` (follow the `ownerDetails.html` delete modal pattern exactly).
  Add a `<script>` block that listens for `show.bs.modal` on `#deleteUserModal`, reads the `data-*` attributes from the triggering button, sets the form action, updates the body text, and shows/hides the cascade checkbox.
- [x] 4.4 `[IMPL]` Add new i18n keys to all nine message files: `deleteUser=Delete User`, `users.delete.confirm.title=Confirm Delete User`, `users.delete.confirm.body=Are you sure you want to delete the account for {0}?`, `users.delete.cascade.label=Also delete the linked owner record (and all their pets and visits)`, `users.delete.success=User account deleted successfully.`, `users.delete.selfDelete.error=You cannot delete your own account.`
- [x] 4.5 `[VERIFY]` Run `./mvnw spring-javaformat:apply` then `./mvnw test -Dtest=AdminUserControllerTests`. Confirm all tests across all four tasks pass.
- [x] 4.6 `[VERIFY]` Run the full test suite `./mvnw test` to confirm no regressions across the entire project.
- [x] 4.7 `[PROOF]` With the app running, open the user list, click the Delete button for an OWNER-role user. Take a screenshot of the modal with the cascade checkbox visible (`15-delete-modal-cascade.png`). Submit without the checkbox checked. Navigate to Find Owners and take a screenshot showing the owner still appears in search results (`15-owner-preserved-after-user-delete.png`).
