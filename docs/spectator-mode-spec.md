# Spec: Live spectator mode

## Objective

Allow anyone with a session ID to watch an active or completed Othello game without joining it. A spectator sees the board, scores, player names, active turn, pass information, and final result in real time, but never receives move hints or an interactive board. Authenticated PVP moves must also be rejected unless the caller owns the submitted color.

## Tech Stack

- Java 17, Spring Boot 2.7.5, Spring MVC/Security, Spring Data JPA
- Server-rendered static HTML/CSS and browser JavaScript
- SockJS/STOMP using the existing `/topic/game-progress/{sessionId}` topic
- JUnit 5, Mockito, MockMvc, H2

## Commands

- Extract skills: `mvn -q skillsjars:extract -Ddir=.agents/skills`
- Compile: `mvn -q -DskipTests test-compile`
- Test: `mvn test`
- Package: `mvn clean install`
- Focused browser-logic tests: `node --test src/test/js/spectator-mode.test.js`

## Project Structure

- `src/main/java/com/project/reversi` — session model, REST controller, and game services
- `src/main/resources/static` — menu, game rendering, spectator flow, and styles
- `src/test/java/com/project/reversi` — controller and service tests
- `src/test/js` — dependency-free tests for spectator URL and read-only-state logic
- `docs` — feature specification and focused manual scenarios

## Code Style

Follow the existing Java formatting and browser-module pattern:

```java
boolean ownsColor = session.getPlayers().stream()
                           .filter(player -> player.getColor() == playerColor)
                           .map(Player::getAccount)
                           .anyMatch(account -> sameUser(account, currentUser));
if (!ownsColor) {
  throw new AccessDeniedException("Player does not own the requested color");
}
```

Browser code remains in an IIFE and exposes only the functions needed by the other existing modules. Names describe behavior (`watchGame`, `isSpectator`, `spectateSessionIdFromSearch`).

## Testing Strategy

- Controller tests prove that session reads remain public and invalid IDs return 404.
- Service/controller tests prove anonymous and non-owner PVP moves are rejected while the owning player can move.
- Existing service tests protect PVP/PVC game behavior.
- Dependency-free Node tests cover direct spectator links and read-only rendering decisions.
- Focused manual scenarios verify the real SockJS/STOMP update path, errors, pass messaging, and completed-game rendering.

## Boundaries

- Always: keep spectator identity separate from player state; reuse the existing session GET and WebSocket topic; run all tests before commit.
- Ask first: add third-party dependencies, change CI, or introduce private-game access rules.
- Never: join a spectator to a game, expose move hints to spectators, allow spectator board actions, or weaken existing authentication.

## Implementation Plan

1. Add authorization-focused tests, then enforce PVP color ownership in the game service/controller.
2. Add spectator-state tests, then implement Watch Game and `?spectate=` entry flows.
3. Make rendering read-only for spectators and surface turn, pass, and final-result status.
4. Verify live updates continue through the existing topic and document focused browser scenarios.
5. Run the complete Maven lifecycle, review the diff, and publish a draft PR.

## Tasks

- [x] Protect PVP move ownership.
  - Acceptance: anonymous callers receive 401; authenticated non-owners receive 403; the active color owner retains existing move behavior.
  - Verify: focused controller and service tests.
  - Files: `GameService.java`, `SessionController.java`, their tests.
- [x] Implement spectator entry and read-only rendering.
  - Acceptance: menu and direct-link flows fetch but never join; no hints or click-driven moves are possible.
  - Verify: dependency-free Node tests and manual browser flow.
  - Files: `index.html`, `app.js`, `menu.js`, `game.js`, `spectator-mode.js`, JS tests.
- [x] Surface spectator game progress.
  - Acceptance: current turn, scores, names, pass information, final result, invalid-session errors, and WebSocket updates are visible.
  - Verify: DTO/controller tests and focused manual scenarios.
  - Files: `GameSession.java`, `GameSessionSummaryDTO.java`, UI resources, tests, manual scenarios.

## Success Criteria

- A session ID entered through Watch Game loads the game without taking a seat.
- `?spectate=<sessionId>` opens the same read-only flow automatically.
- Active and completed sessions render all state required by issue #14.
- Spectators subscribe to existing progress messages but cannot fetch legal moves or submit moves through the UI.
- PVP moves are accepted only from the account that owns the submitted active color.
- Invalid/unavailable IDs show a clear error and leave the user outside game mode.
- Existing PVP and PVC behavior and tests remain green.

## Open Questions

None. Issue #14 defines public spectator access; private games, chat, counts, and replay remain out of scope.
