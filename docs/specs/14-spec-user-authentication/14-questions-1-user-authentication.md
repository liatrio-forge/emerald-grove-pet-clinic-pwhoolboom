# 14 Questions Round 1 - User Authentication

Please answer each question below (select one or more options, or add your own notes). Feel free to add additional context under any question.

## 1. User-to-Owner Relationship

How should a registered user account relate to the Owner records in the system? Currently, "owners" are the clinic clients who have pets.

- [x] (A) **Separate User entity with link to Owner** — A `User` account (username/password/role) links to an existing `Owner` record. Registering means creating both a `User` and a new `Owner` profile at the same time.
- [ ] (B) **Embed auth fields in Owner** — Add `username`, `password`, and `role` columns directly to the existing `owners` table. An owner IS a user.
- [ ] (C) **Separate User entity, no required link** — A `User` exists independently; an admin can manually link a user to an owner later. Regular users see "no data" until linked.
- [ ] (D) Other (describe)

## 2. Registration Flow

What information should a new user provide when registering?

- [x] (A) **Full owner profile at registration** — Username, password, AND all owner fields (first name, last name, address, city, telephone) in a single registration form. Creates both the user account and owner profile.
- [ ] (B) **Account-only registration, profile later** — Register with just username + password (and maybe email). Owner profile details are filled in separately after first login.
- [ ] (C) **Email-based with owner details** — Register with email as username, password, and required owner fields (first/last name, telephone).
- [ ] (D) Other (describe)

## 3. Username/Identifier

What field should serve as the unique login identifier for users?

- [ ] (A) **Username** — A freeform unique username (e.g., "john_smith")
- [x] (B) **Email address** — Email serves as both the unique identifier and login credential
- [ ] (C) **Telephone number** — Use the existing telephone field from the Owner record as the login identifier (maps naturally to the existing `owners` table)
- [ ] (D) Other (describe)

## 4. Admin User Creation

How should the initial admin user be created?

- [x] (A) **Seeded in data.sql** — An admin user (e.g., `admin` / `admin`) is pre-loaded in the sample database data for development/demo purposes
- [ ] (B) **First registered user becomes admin** — The first account to register gets the ADMIN role automatically; all subsequent users are regular owners
- [ ] (C) **Admin account hardcoded in config** — Admin credentials come from `application.properties` (e.g., `petclinic.admin.username`, `petclinic.admin.password`)
- [ ] (D) **Admin promotion by another admin** — Admins can be promoted from regular users via the admin interface (requires admin UI)
- [ ] (E) Other (describe)

## 5. Access Control: Unauthenticated Users

What should happen when a user who is NOT logged in tries to visit owner/pet pages?

- [x] (A) **Redirect to login page** — All owner/pet/visit pages require login; unauthenticated users are redirected to `/login`
- [ ] (B) **Public read, auth to write** — Browsing owners and pets is public; only creating/editing/deleting requires login
- [ ] (C) **Fully public except "My Account"** — The app stays mostly public; authentication only gates a "My Profile" view showing the user's own data
- [ ] (D) Other (describe)

## 6. Access Control: Owner Data Isolation

Once logged in as a regular user/owner, which pages should be restricted to their own data only?

- [x] (A) **Owner detail + pets + visits only** — Users can only view/edit their own owner profile page and their pets/visits. The "Find Owners" search still works but only shows their own results.
- [ ] (B) **Strict isolation** — Regular users cannot use "Find Owners" at all; they are taken directly to their own profile. The owner search/list pages are admin-only.
- [ ] (C) **Profile page only** — A new "My Profile" page shows the user's own data. All other pages remain public/accessible.
- [ ] (D) Other (describe)

## 7. Chatbot Scoping for Regular Users

The chatbot currently has 6 tools (get vets, get pet types, get upcoming visits for owner, get all upcoming visits, get clinic info). How should it behave for a logged-in regular user?

- [x] (A) **Filter owner-specific data automatically** — The chatbot automatically knows who is logged in and only returns data for that user's pets and visits. Vet/pet type/clinic info tools remain unrestricted.
- [ ] (B) **Block owner-related queries entirely** — Regular users can only ask about vets, pet types, and clinic info. Questions about owners/visits/pets are blocked with a message like "Please call the clinic."
- [ ] (C) **Prompt-level instruction only** — Add text to the system prompt telling the chatbot to only discuss the logged-in user's information, relying on the LLM to self-enforce this.
- [ ] (D) Other (describe)

## 8. Navigation and UI for Auth

What UI elements should be added to support the auth flow?

- [x] (A) **Navbar login/logout + register link** — Add "Login" and "Register" links to the top navbar. After login, show username and a "Logout" button.
- [ ] (B) **Dedicated auth pages only** — Just add `/login` and `/register` pages; no persistent UI changes to the main navbar.
- [ ] (C) **Profile dropdown in navbar** — Replace the login link with a profile dropdown after login (showing username, "My Profile", "Logout").
- [ ] (D) Other (describe)

## 9. Session Management

How should authentication state be maintained?

- [x] (A) **Spring Security session-based (default)** — Use Spring Security's built-in HTTP session management. No JWT needed. Works well for a server-rendered Thymeleaf app.
- [ ] (B) **JWT tokens** — Use JSON Web Tokens for stateless authentication. Required if the API will be consumed by external clients.
- [ ] (C) **Remember Me cookie** — Session-based auth with an optional "Remember Me" checkbox on login that persists the session across browser restarts.
- [ ] (D) Other (describe)

## 10. Existing Sample Data Handling

The app has sample data (10 owners, 13 pets, 6 vets) loaded at startup. How should this be handled after auth is introduced?

- [ ] (A) **Leave sample owners unlinked** — Existing sample owners have no user accounts. Only the admin can view them. New registrations create new owners.
- [x] (B) **Seed sample user accounts for sample owners** — Create matching test user accounts in `data.sql` for the existing sample owners (e.g., `george_franklin` / `password`).
- [ ] (C) **Clear sample owner data** — Remove existing sample owner data; the app starts empty and users register to create their profiles.
- [ ] (D) Other (describe)
