package com.project.reversi.controllers;

import com.project.reversi.dto.BoardDTO;
import com.project.reversi.dto.MoveRequestDTO;
import com.project.reversi.dto.MoveResponseDTO;
import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.services.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.Color;

@RestController
@RequestMapping("/api/game")
public class GameController {

  private final GameService gameService;

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

  @PostMapping("/move")
  public ResponseEntity<MoveResponseDTO> makeMove(@RequestBody MoveRequestDTO moveRequest) {
    MoveResponseDTO response = new MoveResponseDTO();
    Color playerColor = "WHITE".equalsIgnoreCase(moveRequest.getColor()) ? Color.WHITE : Color.BLACK;
    try {
      boolean moveResult = gameService.makeMove(
          moveRequest.getSessionId(),
          moveRequest.getRow(),
          moveRequest.getColumn(),
          playerColor
      );
      if (moveResult) {
        response.setMessage("Move successful");
      } else {
        response.setMessage("Invalid move");
      }
      GameSession updatedSession = gameService.getSessionById(moveRequest.getSessionId());
      GameSessionSummaryDTO summary = GameSessionSummaryDTO.fromGameSession(updatedSession);
      response.setSessionSummary(summary);
      return ResponseEntity.ok(response);
    }
    catch (IllegalArgumentException e) {
      response.setMessage("Error: " + e.getMessage());
      return ResponseEntity.badRequest().body(response);
    }
  }

}
