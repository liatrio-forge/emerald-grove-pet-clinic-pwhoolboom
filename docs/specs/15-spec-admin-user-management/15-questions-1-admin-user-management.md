# 15 Questions Round 1 - Admin User Management

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

## 1. Creating New Users

When an admin creates a new OWNER-role user, an `Owner` record must also exist (the User entity has a required OneToOne link to Owner for OWNER accounts). How should admin user creation work?

- [ ] (A) **Owner-first**: Admin can only create user accounts for owners that already exist in the system (pick an existing unlinked Owner record and assign credentials to it).
- [ ] (B) **Combined form**: Admin fills a single form that creates both the User account and a new Owner record at the same time (similar to the existing `/register` flow).
- [x] (C) **Admin-only accounts**: Admin can only create new ADMIN-role users (who have no linked Owner). Creating OWNER-role users stays as self-registration via `/register`.
- [ ] (D) Other (describe)

## 2. Editing User Fields

When an admin edits an existing user, which fields should be editable?

- [ ] (A) **Role only** — Just promote/demote between ADMIN and OWNER. No other fields.
- [ ] (B) **Role + email** — Change role and update the user's login email.
- [ ] (C) **Role + email + password reset** — Admin can also force-set a new password for the user.
- [x] (D) **All fields** — Role, email, password, and the linked Owner record (address, city, telephone, etc.).
- [ ] (E) Other (describe)

## 3. Deleting a User

When an admin deletes a user account, what should happen to the linked Owner record (if any)?

- [ ] (A) **Delete user only** — Remove the User account but keep the Owner record in the system (owner remains in the clinic database, just can't log in).
- [ ] (B) **Delete both** — Remove the User account AND the linked Owner record (and their pets/visits).
- [x] (C) **Ask at delete time** — Show a confirmation dialog that lets the admin choose whether to also delete the Owner record.
- [ ] (D) Other (describe)

## 4. Role Change Safety

If an admin demotes themselves from ADMIN to OWNER (or deletes their own account), they would lose admin access. How should this be handled?

- [ ] (A) **Allow it** — No restriction; admin is responsible for not locking themselves out.
- [ ] (B) **Soft block** — Warn the admin with a confirmation prompt before allowing self-demotion/self-deletion.
- [x] (C) **Hard block** — Prevent an admin from changing their own role or deleting their own account entirely.
- [ ] (D) Other (describe)

## 5. User List / Navigation

Where should the admin user management section live in the UI?

- [x] (A) **New nav item** — Add a dedicated "Manage Users" link in the top navigation bar, visible only to admins.
- [ ] (B) **Under existing nav** — Accessible via a link on an existing admin page (e.g., from the Find Owners page).
- [ ] (C) **Profile/settings area** — Accessible from the admin's own account area (e.g., a dropdown or settings page).
- [ ] (D) Other (describe)

## 6. OWNER Role — Linked Owner Record on Edit

If admin changes a user's role from OWNER to ADMIN, what should happen to their linked Owner record?

- [ ] (A) **Keep the link** — The Owner record stays associated; if the role is changed back to OWNER it relinks automatically.
- [x] (B) **Unlink** — Null out the `owner_id` on the User; the Owner record remains in the clinic database.
- [ ] (C) **Delete the Owner record** — Removing the role removes the associated Owner (and their pets/visits).
- [ ] (D) Other (describe)
