# 15-spec-admin-user-management.md

## Introduction/Overview

This feature gives admin users a dedicated User Management section where they can view all user accounts, create new admin accounts, edit any user's details (including role, email, password, and linked owner information), and delete user accounts. The goal is to give clinic administrators full control over who can access the system and at what permission level without requiring direct database access.

## Goals

- Allow admins to list all user accounts in a single view with key information (email, role, linked owner).
- Allow admins to create new ADMIN-role user accounts directly from the UI.
- Allow admins to promote an OWNER-role user to ADMIN (unlinking their owner record) or update any user's email, password, or owner details.
- Allow admins to delete a user account, with a choice at delete time whether to also remove the linked owner record.
- Prevent admins from modifying or deleting their own account to avoid accidental lockout.

## User Stories

- **As an admin**, I want to see a list of all user accounts so that I can quickly audit who has access to the system.
- **As an admin**, I want to create a new ADMIN-role account so that I can onboard a new clinic administrator without them going through the owner registration flow.
- **As an admin**, I want to edit any user's email, password, role, and owner profile fields so that I can correct mistakes or update their access level.
- **As an admin**, I want to promote an OWNER-role user to ADMIN so that I can grant elevated permissions to a trusted user.
- **As an admin**, I want to delete a user account and optionally remove their linked owner record so that I can revoke access cleanly.

## Demoable Units of Work

### Unit 1: User List and Admin Account Creation

**Purpose:** Gives admins a landing page to see all users and a form to create new ADMIN-role accounts.

**Functional Requirements:**

- The system shall add a "Manage Users" navigation link visible only to users with the ADMIN role.
- The system shall provide a `GET /admin/users` page listing all users with columns: email, role, and linked owner name (or "—" for admin accounts with no owner).
- The system shall provide a `GET /admin/users/new` form with fields: email and password.
- The system shall create the new user with role ADMIN and no linked owner when the form is submitted successfully via `POST /admin/users/new`.
- The system shall reject duplicate email addresses with a validation error on the form.
- The system shall require email (valid format) and password (not blank) on the creation form.
- The system shall restrict `/admin/users/**` routes to ADMIN-role users only, returning 403 for any other authenticated user.

**Proof Artifacts:**

- Screenshot: "Manage Users" nav link visible in the top navigation bar when logged in as admin, absent when logged in as an owner, demonstrates role-gated navigation.
- Screenshot: `/admin/users` page showing the full user list with email, role, and owner columns demonstrates the list endpoint works.
- Screenshot: Successful new-admin creation form submission redirects to the user list with a success flash message, demonstrates end-to-end creation flow.
- Test: `AdminUserControllerTests` — `GET /admin/users` returns 200 for admin and 403 for owner demonstrates access control.

### Unit 2: Edit User (Email, Password, Role, Owner Fields)

**Purpose:** Lets admins update any user's credentials, role, and linked owner profile in one form. Handles the role-change side-effects and blocks self-editing.

**Functional Requirements:**

- The system shall provide a `GET /admin/users/{id}/edit` page showing the user's current email, a blank password field (leave blank to keep existing), role selector, and (for OWNER-role users) editable owner fields: first name, last name, address, city, and telephone.
- The system shall save changes to the User entity (email, hashed password if provided, role) and, for OWNER-role users, save changes to the linked Owner entity via `POST /admin/users/{id}/edit`.
- The system shall BCrypt-hash the new password before saving if the password field is non-blank; it shall leave the existing `passwordHash` unchanged if the password field is left blank.
- The system shall validate that the updated email is unique (excluding the current user's own email).
- When the admin changes a user's role from OWNER to ADMIN, the system shall set the user's `owner` reference to null (unlinking it); the Owner record shall remain in the database.
- When the admin changes a user's role from ADMIN to OWNER, the system shall require owner fields (first name, last name, address, city, telephone) and create a new linked Owner record.
- The system shall return 403 and display an error if an admin attempts to edit their own account (`/admin/users/{currentAdminId}/edit`).
- The system shall validate all owner fields with the same constraints as the existing owner creation form (telephone must be 10 digits, all required fields not blank).

**Proof Artifacts:**

- Screenshot: Edit form for an OWNER-role user shows all fields (email, password, role, owner details) pre-populated, demonstrates the form renders correctly.
- Screenshot: After changing a user's role from OWNER to ADMIN, the user list shows their role as ADMIN and owner column as "—", demonstrates role-change and owner unlink.
- Test: `AdminUserControllerTests` — POST to edit own account returns 403, demonstrates self-edit hard block.
- Test: `AdminUserControllerTests` — password field left blank on edit does not change stored `passwordHash`, demonstrates password preservation logic.

### Unit 3: Delete User (with Optional Owner Cascade)

**Purpose:** Lets admins remove a user account with a clear choice at delete time about whether to also remove the linked owner record. Blocks self-deletion.

**Functional Requirements:**

- The system shall show a delete button for each user on the `/admin/users` list page (except for the currently logged-in admin's own row).
- The system shall display a confirmation modal before deletion. For OWNER-role users with a linked owner, the modal shall include a checkbox labelled "Also delete the linked owner record (and all their pets and visits)".
- The system shall delete only the User account when the checkbox is unchecked, leaving the Owner, pets, and visits intact in the database.
- The system shall delete the User account, the linked Owner record, and all associated pets and visits (via cascade) when the checkbox is checked.
- The system shall return 403 if an admin attempts to delete their own account via `POST /admin/users/{id}/delete`.
- The system shall redirect to `/admin/users` with a success flash message after a successful deletion.

**Proof Artifacts:**

- Screenshot: Delete confirmation modal for an OWNER-role user showing the "also delete owner" checkbox, demonstrates the UX for cascade choice.
- Screenshot: After deleting a user without the cascade option, the owner still appears on the Find Owners page, demonstrates the owner record is preserved.
- Test: `AdminUserControllerTests` — POST delete of own account returns 403, demonstrates self-deletion hard block.
- Test: `AdminUserControllerTests` — POST delete with cascade=true removes the linked owner, demonstrates cascade deletion.

## Non-Goals (Out of Scope)

1. **Self-service account management**: This spec only covers admin management of other users' accounts. User profile editing for non-admin users (e.g., owners updating their own email/password) is out of scope.
2. **Password reset via email**: No "forgot password" email flow or token-based reset is included. Admin can force-set a password via the edit form.
3. **Audit log**: No history or change log of user management actions is included.
4. **Bulk operations**: No bulk delete, bulk role change, or CSV import of users is included.
5. **Session invalidation**: When a user is deleted or demoted, any active sessions for that user are not forcibly invalidated. This is a known limitation.
6. **Creating OWNER-role accounts from scratch**: Admin can only create new ADMIN-role accounts directly. New OWNER accounts are created through the existing `/register` self-registration flow.

## Design Considerations

- The user list page (`/admin/users`) should follow the same Bootstrap 5 / `liatrio-table` table style used on the Find Owners page (`owners/ownersList.html`).
- The edit form should conditionally show or hide the owner detail fields based on the selected role, ideally with a small JavaScript toggle so the fields appear/disappear when the role selector changes.
- The delete confirmation modal should follow the same Bootstrap 5 modal pattern used on the Delete Pet feature (`ownerDetails.html` `#deletePetModal`).
- The "Manage Users" nav link should only be rendered for authenticated admin users, using the existing Thymeleaf Security dialect (`sec:authorize="hasRole('ADMIN')"` or a model attribute flag, consistent with how the chat widget is conditionally shown).
- Flash messages (success/error) should follow the existing `alert-success` / `alert-danger` pattern used on `ownerDetails.html`.

## Repository Standards

- **Controller pattern**: `@Controller` class in `src/main/java/.../security/` package, following the same structure as `OwnerController.java` (constructor injection, `@InitBinder`, `@ModelAttribute`, separated GET/POST mappings).
- **Test pattern**: `@WebMvcTest` + `@Import(WebMvcTestSecurityConfig.class)` + `@WithMockUser(roles = "ADMIN")`, with `@MockitoBean` for repository dependencies. All POST tests must use `.with(csrf())`.
- **TDD**: Tests must be written before implementation per the project's Strict TDD mandate. Every controller method gets a corresponding test before the method is written.
- **Formatting**: All Java files must pass `./mvnw spring-javaformat:apply` before committing.
- **i18n**: Any new user-facing strings should be added to all message property files under `src/main/resources/messages/` (en, es, de, fa, ko, pt, ru, tr).
- **Commit style**: Conventional commits (`feat:`, `fix:`, `test:`).

## Technical Considerations

- **Access control**: All `/admin/users/**` routes must be added to `SecurityConfig` as ADMIN-only, or enforced in the controller via `Authentication auth` parameter checks (consistent with existing role checks in `OwnerController`). Prefer adding a `requestMatchers("/admin/**").hasRole("ADMIN")` rule in `SecurityConfig` for clarity.
- **Self-edit/self-delete guard**: The controller must compare the `ownerId` of the target user against the currently authenticated user's ID (from the `Authentication` principal) and return a 403 or redirect with an error flash.
- **Password handling on edit**: The edit form's password field must be optional. The controller should only BCrypt-encode and update `passwordHash` if the submitted password field is non-blank.
- **Role change — OWNER to ADMIN**: Set `user.setOwner(null)` before saving; do not delete the Owner entity.
- **Role change — ADMIN to OWNER**: The controller must create a new `Owner` entity from the submitted owner fields, save it, then set `user.setOwner(savedOwner)` before saving the user.
- **Cascade delete**: Use JPA cascade on the `Owner → Pet → Visit` relationships (already present in the data model) or issue explicit delete calls for pets and visits before deleting the owner. Verify the existing cascade settings in `Owner.java` before implementing.
- **Email uniqueness on edit**: When validating uniqueness, the query `findByEmail()` must exclude the user being edited to avoid a false duplicate error on unchanged emails.
- **Template path**: New templates under `src/main/resources/templates/admin/` (e.g., `admin/userList.html`, `admin/createOrUpdateUserForm.html`).

## Security Considerations

- All `/admin/users/**` routes must require ADMIN role; any unauthenticated or OWNER-role request must be denied (403, not redirect to login, to avoid leaking that the endpoint exists to non-admins).
- Password values must never be echoed back into rendered HTML (no `th:value` on the password input).
- The `passwordHash` field must never be exposed in any model attribute passed to a Thymeleaf template.
- CSRF protection must remain enabled; all POST forms must use Thymeleaf's `th:action` so the CSRF token is injected automatically.
- Proof artifact screenshots must not capture any real password values.

## Success Metrics

1. **All unit tests pass**: `AdminUserControllerTests` covers every controller endpoint (GET list, GET new, POST new, GET edit, POST edit, POST delete) with admin and non-admin roles — 0 failures.
2. **Access control enforced**: OWNER-role users and unauthenticated users receive 403/redirect for all `/admin/users/**` routes.
3. **Self-protection works**: Attempting to edit or delete the currently logged-in admin account returns an error in all test scenarios.
4. **No orphaned data**: After a cascade delete, the owner, their pets, and their visits are no longer present in the database; after a non-cascade delete, the owner record remains.

## Open Questions

1. **ADMIN → OWNER conversion with no owner data**: When changing an existing ADMIN-role user (who has no owner record) to OWNER role, the edit form must collect owner fields. Should the edit form always show owner fields (hidden when role is ADMIN, visible when role is OWNER) and require them when OWNER is selected? Or should ADMIN → OWNER conversion be restricted and require a separate flow? The spec currently requires the edit form to show owner fields for this case; confirm this is acceptable before implementation.
2. **Cascade delete and pets**: If an owner record being cascade-deleted has pets with future visit appointments, should the system warn the admin or block deletion? This is not currently in scope but may need a follow-up spec.
3. **Pagination on user list**: If the clinic grows to have many users, the flat list at `/admin/users` may become unwieldy. Pagination is not in scope for this spec but should be considered for a future iteration.
