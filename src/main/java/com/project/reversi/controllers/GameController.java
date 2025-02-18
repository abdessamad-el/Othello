package com.project.reversi.controllers;

import com.project.reversi.dto.BoardDTO;
import com.project.reversi.dto.MoveRequestDTO;
import com.project.reversi.dto.MoveResponseDTO;
import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.MoveResult;
import com.project.reversi.services.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;

@RestController
@RequestMapping("/api/game")
public class GameController {

  private final GameService gameService;
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }


  @GetMapping("/board")
  public ResponseEntity<BoardDTO> getBoard(@RequestParam String sessionId) {
    GameSession session = gameService.getSessionById(sessionId);
    if (session == null) {
      return ResponseEntity.notFound().build();
    }
    GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(session);
    return ResponseEntity.ok(summary.getBoard());
  }

  /**
   * Processes a move within a game session.
   * Expects a JSON payload with sessionId, row, column, and color.
   * Returns a MoveResponseDTO with a message and a session summary.
   */
  @PostMapping("/move")
  public ResponseEntity<MoveResponseDTO> makeMove(@RequestBody MoveRequestDTO moveRequest) {
    MoveResponseDTO response = new MoveResponseDTO();
    Color playerColor = "WHITE".equalsIgnoreCase(moveRequest.getColor()) ? Color.WHITE : Color.BLACK;
    try {
      MoveResult result = gameService.makeMove(
          moveRequest.getSessionId(),
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
        case NO_MOVES_AVAILABLE:
          response.setMessage("No valid moves available; turn passed");
          break;
        case GAME_FINISHED:
          response.setMessage("Game is finished, you can't do more moves");
          break;
        case WRONG_TURN:
          response.setMessage("It's not your turn");
          break;
      }
      GameSession updatedSession = gameService.getSessionById(moveRequest.getSessionId());
      GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(updatedSession);
      response.setSessionSummary(summary);
      return ResponseEntity.ok(response);
    }
    catch (IllegalArgumentException e) {
      logger.error("Error processing move: {}", e.getMessage());
      response.setMessage("Error: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

}
