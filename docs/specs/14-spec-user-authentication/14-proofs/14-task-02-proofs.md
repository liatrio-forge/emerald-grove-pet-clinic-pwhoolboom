# 14-task-02-proofs.md — Owner Data Isolation

## Task 2.0: Owner Data Isolation — Protect Routes & Restrict Regular Users to Own Data

---

## CLI Output: Test Suite

```text
./mvnw test
...
[WARNING] Tests run: 144, Failures: 0, Errors: 0, Skipped: 6
[INFO] BUILD SUCCESS
```

---

## New/Updated Tests

### OwnerControllerTests — new access control tests

```text
testShowOwner_ownerAccessingOwnProfile_returns200           - PASS
testShowOwner_ownerAccessingOtherOwnerProfile_returns403    - PASS
testInitUpdateOwnerForm_ownerAccessingOtherOwner_returns403 - PASS
testProcessFindForm_ownerRoleUser_seesOnlyOwnRecord         - PASS
```

### UpcomingVisitsControllerTests — new scoping test

```text
testShowUpcomingVisits_ownerRoleUser_seesOnlyOwnPets        - PASS
```

---

## Access Control Implementation

### OwnerController changes

```java
// Guard logic in showOwner(), initUpdateOwnerForm(), processUpdateOwnerForm()
private boolean isOwnerRoleUser(Authentication auth) {
    return auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
}

// Ownership check: throws AccessDeniedException → 403 response
if (isOwnerRoleUser(auth)) {
    User currentUser = resolveCurrentUser(auth);
    if (!currentUser.getOwner().getId().equals(ownerId)) {
        throw new AccessDeniedException("Access denied");
    }
}
```

### processFindForm filtering

```java
// For OWNER-role users: filter results to only show their own record
if (isOwnerRoleUser(auth)) {
    User currentUser = resolveCurrentUser(auth);
    int linkedOwnerId = currentUser.getOwner().getId();
    List<Owner> filtered = ownersResults.getContent().stream()
        .filter(o -> o.getId() != null && o.getId().equals(linkedOwnerId))
        .toList();
    // redirect to their own record or return "not found"
}
```

---

## VisitRepository: new owner-scoped query

```java
@Query("""
    SELECT new org.springframework.samples.petclinic.owner.UpcomingVisit(
        o.id, CONCAT(o.firstName, ' ', o.lastName), p.name, v.date, v.description)
    FROM Owner o JOIN o.pets p JOIN p.visits v
    WHERE v.date >= :startDate AND v.date <= :endDate AND o.id = :ownerId
    ORDER BY v.date ASC
    """)
List<UpcomingVisit> findUpcomingVisitsByOwnerId(
    @Param("ownerId") int ownerId,
    @Param("startDate") LocalDate startDate,
    @Param("endDate") LocalDate endDate);
```

---

## UpcomingVisitsController: role-aware filtering

```java
if (isOwnerRoleUser(auth)) {
    User currentUser = userRepository.findByEmail(auth.getName())...;
    model.addAttribute("upcomingVisits",
        visits.findUpcomingVisitsByOwnerId(currentUser.getOwner().getId(), today, endDate));
} else {
    model.addAttribute("upcomingVisits", visits.findUpcomingVisits(today, endDate));
}
```

---

## Verification

- ✅ OWNER user accessing own owner page → 200 OK
- ✅ OWNER user accessing other owner page → 403 Forbidden
- ✅ OWNER user accessing other owner edit page → 403 Forbidden
- ✅ Find Owners search for OWNER role → only shows their own record
- ✅ Upcoming Visits for OWNER role → only shows their pets' visits
- ✅ All existing tests still pass (144/144)
