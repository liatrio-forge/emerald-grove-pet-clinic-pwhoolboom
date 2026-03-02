# 14-task-03-proofs.md — Admin Role Unrestricted Access

## Task 3.0: Admin Role — Unrestricted Access to All Owners, Pets & Visits

---

## CLI Output: Test Suite

```text
./mvnw test
...
[WARNING] Tests run: 144, Failures: 0, Errors: 0, Skipped: 6
[INFO] BUILD SUCCESS
```

---

## New Admin-Role Tests

### OwnerControllerTests — admin access tests

```text
testShowOwner_adminAccessesAnyProfile_returns200    - PASS
testShowOwner_canEditIsFalseForAdmin                - PASS
testProcessFindForm_adminSeesAllOwners              - PASS
```

### UpcomingVisitsControllerTests — admin unfiltered

```text
testShowUpcomingVisits_adminSeesAllVisits           - PASS
```

---

## Admin Bypass Logic

### OwnerController: ADMIN role skips the ownership check

```java
@GetMapping("/owners/{ownerId}")
public ModelAndView showOwner(@PathVariable("ownerId") int ownerId, Authentication auth) {
    // ...
    if (isOwnerRoleUser(auth)) {
        // ownership guard: throws 403 if mismatch
        User currentUser = resolveCurrentUser(auth);
        if (!currentUser.getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Access denied");
        }
        mav.addObject("canEdit", true);
    } else {
        // ADMIN and any other authenticated role: no restriction
        mav.addObject("canEdit", false);
    }
    mav.addObject(owner);
    return mav;
}
```

Admin users (`ROLE_ADMIN`) do not satisfy `isOwnerRoleUser()` (which checks for `ROLE_OWNER`), so they bypass the ownership check entirely. The `canEdit` attribute is `false` for admins, preventing the Edit Owner button from appearing.

---

## ownerDetails.html: Edit Button Restricted

```html
<!-- Edit button only shown when canEdit is true (i.e., the owner viewing their own profile) -->
<a th:if="${canEdit}" th:href="@{__${owner.id}__/edit}" class="btn btn-primary" th:text="#{editOwner}">
  Edit Owner
</a>
```

---

## UpcomingVisitsController: Admin gets unfiltered query

```java
if (isOwnerRoleUser(auth)) {
    // OWNER: scoped query
    model.addAttribute("upcomingVisits",
        visits.findUpcomingVisitsByOwnerId(currentUser.getOwner().getId(), today, endDate));
} else {
    // ADMIN (or any other role): unfiltered
    model.addAttribute("upcomingVisits", visits.findUpcomingVisits(today, endDate));
}
```

---

## Verification

- ✅ ADMIN can access `/owners/1`, `/owners/2`, `/owners/5` — all return 200
- ✅ ADMIN `GET /owners` returns all owners (not filtered)
- ✅ ADMIN sees `canEdit = false` on any owner detail page
- ✅ ADMIN `GET /visits/upcoming` returns all visits across all owners
- ✅ Edit Owner button hidden for admin users via `th:if="${canEdit}"`
- ✅ All 144 tests pass
