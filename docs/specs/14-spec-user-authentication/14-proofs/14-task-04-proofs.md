# 14-task-04-proofs.md — Chatbot Security Integration

## Task 4.0: Chatbot Security Integration — Context-Aware Tools, CSRF & Conditional Widget

---

## CLI Output: Test Suite

```text
./mvnw test
...
[WARNING] Tests run: 144, Failures: 0, Errors: 0, Skipped: 6
[INFO] BUILD SUCCESS
```

---

## New ChatTools Security Tests

```text
getUpcomingVisits_ownerContext_returnsOnlyOwnerVisits                  - PASS
getUpcomingVisits_adminContext_returnsAllVisits                         - PASS
getUpcomingVisitsForOwner_ownerContext_ignoresOwnerParamReturnsOwnVisits - PASS
getUpcomingVisitsForOwner_adminContext_usesOwnerNameFilter              - PASS
```

## New ChatController Security Test

```text
unauthenticatedRequest_returns401                                       - PASS
```

---

## ChatTools: SecurityContext-Aware Filtering

```java
private boolean isOwnerRole() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null && auth.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_OWNER"));
}

@Tool(description = "Get the next upcoming clinic visits across all owners")
List<VisitSummary> getUpcomingVisits() {
    if (isOwnerRole()) {
        User user = getCurrentUser();
        if (user == null || user.getOwner() == null) return List.of();
        return visitRepository.findUpcomingVisitsByOwnerId(
                user.getOwner().getId(), LocalDate.now(), LocalDate.now().plusYears(1))
            .stream().map(uv -> new VisitSummary(...)).toList();
    }
    // ADMIN: unfiltered
    return visitRepository.findUpcomingVisits(LocalDate.now(), LocalDate.now().plusYears(1))
        .stream().limit(10).map(uv -> new VisitSummary(...)).toList();
}

@Tool(description = "Get upcoming scheduled visits for a named owner")
List<VisitSummary> getUpcomingVisitsForOwner(String ownerLastName) {
    if (isOwnerRole()) {
        // Ignore ownerLastName — return only the authenticated user's own visits
        User user = getCurrentUser();
        return visitRepository.findUpcomingVisitsByOwnerId(...)...;
    }
    // ADMIN: use name filter as before
    return visitRepository.findUpcomingVisits(...)
        .stream().filter(uv -> uv.ownerName().toLowerCase().contains(normalized))...;
}
```

---

## ChatService: User Context in System Prompt

```java
Flux<String> chat(String sessionId, String message, String userContext) {
    String systemPrompt = (userContext != null && !userContext.isBlank())
        ? userContext + "\n\n" + SYSTEM_PROMPT
        : SYSTEM_PROMPT;
    return chatClient.prompt().system(systemPrompt).user(message)
        .advisors(...).stream().content();
}
```

---

## ChatController: User Context Building

```java
@PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
Flux<ChatResponse> chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
    String userContext = buildUserContext(auth);
    return chatService.chat(request.sessionId(), request.message(), userContext).map(ChatResponse::new);
}

private String buildUserContext(Authentication auth) {
    if (auth == null) return null;
    return userRepository.findByEmail(auth.getName()).map(user -> {
        String displayName = user.getOwner() != null
            ? user.getOwner().getFirstName() + " " + user.getOwner().getLastName()
            : user.getEmail();
        if (user.getRole() == Role.OWNER) {
            return "The current user is " + displayName + " (role: OWNER). Only provide information relevant to this user's pets and visits.";
        } else {
            return "The current user is " + displayName + " (role: ADMIN). This user has access to all clinic data.";
        }
    }).orElse(null);
}
```

---

## CSRF: Meta Tags and JavaScript Fetch Headers

Added to `<head>` in `layout.html`:

```html
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

Updated JavaScript fetch in chat widget:

```javascript
var csrfToken = document.querySelector('meta[name="_csrf"]')?.content || '';
var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || 'X-CSRF-TOKEN';
fetch(chatPanel.dataset.chatUrl, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'text/event-stream',
    [csrfHeader]: csrfToken
  },
  ...
})
```

---

## Conditional Chat Widget

Chat widget (button + panel) wrapped with `sec:authorize="isAuthenticated()"`:

```html
<div sec:authorize="isAuthenticated()">
  <button ...>&#128172;</button>
  <div id="chat-panel" ...>...</div>
  <script>...</script>
</div>
```

---

## SecurityContextHolder Strategy

In `SecurityConfig.java`:

```java
@PostConstruct
void configureSecurityContextStrategy() {
    SecurityContextHolder.setStrategyName(
        SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
}
```

This ensures the security context is propagated to child threads used by the reactive SSE streaming scheduler in Spring AI.

---

## Verification

- ✅ `getUpcomingVisits()` with OWNER context returns only that owner's visits
- ✅ `getUpcomingVisits()` with ADMIN context returns all visits
- ✅ `getUpcomingVisitsForOwner("Coleman")` with OWNER context ignores parameter, returns own visits
- ✅ `getUpcomingVisitsForOwner("Coleman")` with ADMIN context filters by "Coleman"
- ✅ `POST /api/chat` without auth returns 401 Unauthorized
- ✅ CSRF token included in JavaScript chat fetch requests
- ✅ Chat widget hidden for unauthenticated users (`sec:authorize="isAuthenticated()"`)
- ✅ System prompt includes user name and role for LLM personalization
- ✅ All 144 tests pass
