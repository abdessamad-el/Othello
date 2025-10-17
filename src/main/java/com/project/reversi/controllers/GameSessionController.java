package com.project.reversi.controllers;

import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.Player;
import com.project.reversi.services.GameSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;

@RestController
@RequestMapping("/api/session")
public class GameSessionController {

  private final GameSessionService gameSessionService;

  public GameSessionController(GameSessionService gameSessionService) {
    this.gameSessionService = gameSessionService;
  }

  /**
   * Creates a new game session.
   * For PLAYER_VS_COMPUTER mode, the computer player is automatically added.
   * For PLAYER_VS_PLAYER mode, a placeholder is created for the second player.
   * <p>
   * Example call: POST /api/session/create?gameType=PLAYER_VS_PLAYER&color=WHITE
   *
   * @param gameType the type of game (PLAYER_VS_PLAYER or PLAYER_VS_COMPUTER)
   * @param color    a string representing the creator's color ("WHITE" or "BLACK")
   * @return the created GameSession summary in JSON format
   */
  @PostMapping("/create")
  public ResponseEntity<GameSessionSummaryDTO> createSession(
      @RequestParam GameType gameType,
      @RequestParam String color
  ) {
    Color playerColor = "WHITE".equalsIgnoreCase(color) ? Color.WHITE : Color.BLACK;
    Player creator = new Player(playerColor);
    GameSession session = gameSessionService.createGameSession(gameType, creator);
    GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(session);
    return ResponseEntity.ok(summary);
  }


  /**
   * Allows a second player to join an existing game session.
   * This is only valid for PLAYER_VS_PLAYER mode.
   * <p>
   * Example call: POST /api/session/{sessionId}/join?color=BLACK
   *
   * @param sessionId the unique identifier for the game session
   * @param color     a string representing the joining player's color ("WHITE" or "BLACK")
   * @return the updated GameSession summary, or an error if the session is not joinable
   */
  @PostMapping("/{sessionId}/join")
  public ResponseEntity<GameSessionSummaryDTO> joinSession(@PathVariable String sessionId) {
    // Retrieve the session
    GameSession session = gameSessionService.getSessionById(sessionId);
    if (session == null) {
      return ResponseEntity.notFound().build();
    }

    // Determine the color for the joining player based on the first player's color
    String assignedColor;
    if (session.getPlayerAtSeat(0).getColor().equals(Color.WHITE)) {
      assignedColor = "BLACK";
    } else {
      assignedColor = "WHITE";
    }

    // Create the joining player with the assigned color
    Player joiningPlayer = new Player("WHITE".equalsIgnoreCase(assignedColor) ? Color.WHITE : Color.BLACK);

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
   * <p>
   * Example call: GET /api/session/{sessionId}
   *
   * @param sessionId the unique identifier for the game session
   * @return the GameSession summary in JSON format, or 404 if not found
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
