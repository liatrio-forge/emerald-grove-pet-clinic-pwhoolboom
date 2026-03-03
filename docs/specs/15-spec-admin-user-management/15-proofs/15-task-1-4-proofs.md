# Spec 15 - Tasks 1–4 Proof Artifacts

## Task 1 — Security Config, Navigation, and User List

### Task 1 Test Results

```text
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
```

All three access-control tests from task 1 pass:

- `testGetUserList_asAdmin_returns200` — GET /admin/users returns 200 for ADMIN role
- `testGetUserList_asOwner_returns403` — GET /admin/users returns 403 for OWNER role
- `testGetUserList_unauthenticated_redirectsToLogin` — GET /admin/users redirects unauthenticated

### Security Config Change

`SecurityConfig.java` now includes:

```java
.requestMatchers("/admin/**")
.hasRole("ADMIN")
```

Added before `.anyRequest().authenticated()` to enforce ADMIN-only access to all admin routes.

### Navigation Change

`layout.html` now includes a "Manage Users" nav link wrapped in `sec:authorize="hasRole('ADMIN')"`:

```html
<li sec:authorize="hasRole('ADMIN')"
    th:replace="~{::menuItem ('/admin/users','admin','manage users','users',#{manageUsers})}">
```

### Task 1 Screenshots

Screenshots should be captured from the running application:

- `15-nav-admin-visible.png` — nav bar showing "Manage Users" when logged in as admin@petclinic.com
- `15-nav-owner-hidden.png` — nav bar without "Manage Users" when logged in as an owner
- `15-user-list.png` — /admin/users page with email, role, and owner columns

---

## Task 2 — Create New Admin User

### Task 2 Test Results

All four creation tests pass:

- `testGetNewAdminForm_returns200` — GET /admin/users/new returns 200 and creates adminUserForm model
- `testPostNewAdmin_validData_redirectsToList` — POST with valid data redirects to /admin/users
- `testPostNewAdmin_duplicateEmail_showsFormError` — POST with duplicate email shows form error
- `testPostNewAdmin_blankPassword_showsFormError` — POST with blank password shows field error

### Task 2 Screenshots

- `15-create-admin-form.png` — /admin/users/new form with email and password fields
- `15-create-admin-success.png` — user list after successful creation showing success flash and new account

---

## Task 3 — Edit User (All Fields, Role Change, Self-Edit Block)

### Task 3 Test Results

All seven edit tests pass:

- `testGetEditForm_asAdmin_returns200` — GET /admin/users/2/edit returns 200
- `testGetEditForm_ownAccount_returns403` — GET own account returns 403
- `testPostEdit_validData_redirectsToList` — POST valid data redirects
- `testPostEdit_blankPassword_preservesPasswordHash` — blank password leaves passwordHash unchanged
- `testPostEdit_ownerToAdmin_unlinksOwner` — role change OWNER→ADMIN sets owner to null
- `testPostEdit_ownAccount_returns403` — POST to own account returns 403
- `testPostEdit_duplicateEmail_showsFormError` — duplicate email shows form error

### Task 3 Screenshots

- `15-edit-owner-user-form.png` — edit form for OWNER-role user with all fields pre-populated
- `15-role-change-owner-to-admin.png` — user list after role change showing ADMIN and "—" in owner column

---

## Task 4 — Delete User (with Optional Owner Cascade)

### Task 4 Test Results

All four delete tests pass:

- `testPostDelete_asAdmin_redirectsToList` — DELETE redirects to /admin/users
- `testPostDelete_ownAccount_returns403` — DELETE own account returns 403
- `testPostDelete_withoutCascade_preservesOwner` — ownerRepository.delete() NOT called without cascade
- `testPostDelete_withCascade_deletesOwner` — ownerRepository.delete(owner) IS called with cascade=true

### Full Test Suite

```text
Tests run: 165, Failures: 0, Errors: 0, Skipped: 6
BUILD SUCCESS
```

Zero regressions across the entire project.

### Task 4 Screenshots

- `15-delete-modal-cascade.png` — delete confirmation modal for OWNER-role user showing cascade checkbox
- `15-owner-preserved-after-user-delete.png` — owner still in Find Owners page after non-cascade delete
