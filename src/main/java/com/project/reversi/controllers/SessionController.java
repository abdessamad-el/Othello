package com.project.reversi.controllers;

import com.project.reversi.dto.BoardDTO;
import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.dto.MoveDTO;
import com.project.reversi.dto.MoveRequestDTO;
import com.project.reversi.dto.MoveResponseDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MoveResult;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.User;
import com.project.reversi.services.GameService;
import com.project.reversi.services.GameSessionService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

  private final GameSessionService gameSessionService;
  private final GameService gameService;
  private static final Logger logger = LoggerFactory.getLogger(SessionController.class);
  private final SimpMessagingTemplate simpMessagingTemplate;

  public SessionController(
      GameSessionService gameSessionService,
      GameService gameService,
      SimpMessagingTemplate simpMessagingTemplate
  ) {
    this.gameSessionService = gameSessionService;
    this.gameService = gameService;
    this.simpMessagingTemplate = simpMessagingTemplate;
  }

  /**
   * Creates a new game session.
   * PLAYER_VS_PLAYER sessions require authentication; the authenticated user occupies seat 0.
   */
  @PostMapping("")
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


  @GetMapping("/{sessionId}/board")
  public ResponseEntity<BoardDTO> getBoard(@PathVariable String sessionId) {
    GameSession session = gameService.getSessionById(sessionId);
    if (session == null) {
      return ResponseEntity.notFound().build();
    }
    GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(session);
    return ResponseEntity.ok(summary.getBoard());
  }


  @GetMapping("/{sessionId}/possible-moves")
  public ResponseEntity<List<MoveDTO>> getPossibleMoves(
      @PathVariable String sessionId,
      @RequestParam PlayerColor color
  ) {
    // Validate session
    GameSession session = gameService.getSessionById(sessionId);
    if (session == null) {
      return ResponseEntity.badRequest().build();
    }
    List<MoveDTO> validMoves = session
        .computeValidMoves(color)
        .stream()
        .map(p -> new MoveDTO(p.row(), p.col()))
        .collect(
            Collectors.toList());
    return ResponseEntity.ok(validMoves);
  }

  /**
   * Processes a move within a game session.
   * Expects a JSON payload with sessionId, row, column, and color.
   * Returns a MoveResponseDTO with a message and a session summary.
   */
  @PostMapping("/{sessionId}/moves")
  public ResponseEntity<MoveResponseDTO> makeMove(
      @PathVariable String sessionId,
      @RequestBody MoveRequestDTO moveRequest
  ) {
    MoveResponseDTO response = new MoveResponseDTO();
    PlayerColor playerColor = moveRequest.getColor();
    try {
      MoveResult result = gameService.makeMove(
          sessionId,
          moveRequest.getRow(),
          moveRequest.getColumn(),
          playerColor
      );

      switch (result) {
        case SUCCESS:
          response.setMessage("Move successful");
          break;
        case INVALID_MOVE:
          response.setMessage("Invalid move");
          break;
        case GAME_FINISHED:
          response.setMessage("Game is finished, you can't do more moves");
          break;
        case WRONG_TURN:
          response.setMessage("It's not your turn");
          break;
      }
      GameSession updatedSession = gameService.getSessionById(sessionId);
      GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(updatedSession);
      response.setSessionSummary(summary);
      simpMessagingTemplate.convertAndSend(
          "/topic/game-progress/" + response.getSessionSummary().getSessionId(),
          response
      );
      return ResponseEntity.ok(response);
    }
    catch (IllegalArgumentException e) {
      logger.error("Error processing move: {}", e.getMessage());
      response.setMessage("Error: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }
}
