package com.project.reversi.controllers;

import com.project.reversi.dto.BoardDTO;
import com.project.reversi.dto.MoveDTO;
import com.project.reversi.dto.MoveRequestDTO;
import com.project.reversi.dto.MoveResponseDTO;
import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.MoveResult;
import com.project.reversi.services.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/game")
public class GameController {

  private final GameService gameService;
  private static final Logger logger = LoggerFactory.getLogger(GameController.class);

  @Autowired
  private SimpMessagingTemplate simpMessagingTemplate;

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


  @PostMapping("/possible-moves")
  public ResponseEntity<List<MoveDTO>> getPossibleMoves(@RequestBody MoveRequestDTO moveRequest) {
    // Validate session
    GameSession session = gameService.getSessionById(moveRequest.getSessionId());
    if (session == null) {
      return ResponseEntity.badRequest().build();
    }
    Color playerColor = "WHITE".equalsIgnoreCase(moveRequest.getColor()) ? Color.WHITE : Color.BLACK;
    List<MoveDTO> validMoves = session.getBoard()
                                      .ComputeValidMoves(playerColor)
                                      .stream()
                                      .map(move -> new MoveDTO(move[0], move[1]))
                                      .collect(
                                          Collectors.toList());
    return ResponseEntity.ok(validMoves);
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
          playerColor,
          moveRequest.getPass()
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
      simpMessagingTemplate.convertAndSend("/topic/game-progress/" + response.getSessionSummary().getSessionId(), response);
      return ResponseEntity.ok(response);
    }
    catch (IllegalArgumentException e) {
      logger.error("Error processing move: {}", e.getMessage());
      response.setMessage("Error: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

}
