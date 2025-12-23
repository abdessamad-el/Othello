package com.project.reversi.controllers;

import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.User;
import com.project.reversi.services.GameSessionService;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class GameSessionController {

  private final GameSessionService gameSessionService;

  public GameSessionController(GameSessionService gameSessionService) {
    this.gameSessionService = gameSessionService;
  }

  /**
   * Creates a new game session.
   * PLAYER_VS_PLAYER sessions require authentication; the authenticated user occupies seat 0.
   */
  @PostMapping("/create")
  @PreAuthorize("#gameType != T(com.project.reversi.model.GameType).PLAYER_VS_PLAYER || isAuthenticated()")
  public ResponseEntity<GameSessionSummaryDTO> createSession(
      @RequestParam GameType gameType,
      @RequestParam PlayerColor color,
      @AuthenticationPrincipal User currentUser
  ) {
    Player creator = new Player(color);
    if (currentUser != null) {
      creator.setAccount(currentUser);
      creator.setNickName(currentUser.getUsername());
    }
    GameSession session = gameSessionService.createGameSession(gameType, creator);
    GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(session);
    return ResponseEntity.ok(summary);
  }

  /**
   * Allows a second player to join an existing PLAYER_VS_PLAYER session.
   */
  @PostMapping("/{sessionId}/join")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<GameSessionSummaryDTO> joinSession(
      @PathVariable String sessionId,
      @AuthenticationPrincipal User currentUser
  ) {
    GameSession session = gameSessionService.getSessionById(sessionId);
    if (session == null) {
      return ResponseEntity.notFound().build();
    }
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    boolean alreadySeated = session.getPlayers().stream()
        .filter(Objects::nonNull)
        .anyMatch(player -> player.getAccount() != null
            && player.getAccount().getId() != null
            && player.getAccount().getId().equals(currentUser.getId()));
    if (alreadySeated) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    String assignedColor = session.getPlayerAtSeat(0).getColor() == PlayerColor.WHITE ? "BLACK" : "WHITE";
    Player joiningPlayer = new Player("WHITE".equalsIgnoreCase(assignedColor) ? PlayerColor.WHITE : PlayerColor.BLACK);
    joiningPlayer.setAccount(currentUser);
    joiningPlayer.setNickName(currentUser.getUsername());

    try {
      session = gameSessionService.joinGameSession(sessionId, joiningPlayer);
      GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(session);
      return ResponseEntity.ok(summary);
    }
    catch (IllegalArgumentException | IllegalStateException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  /**
   * Retrieves a game session by its session ID.
   */
  @GetMapping("/{sessionId}")
  public ResponseEntity<GameSessionSummaryDTO> getSession(@PathVariable String sessionId) {
    GameSession session = gameSessionService.getSessionById(sessionId);
    if (session == null) {
      return ResponseEntity.notFound().build();
    }
    GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(session);
    return ResponseEntity.ok(summary);
  }
}
