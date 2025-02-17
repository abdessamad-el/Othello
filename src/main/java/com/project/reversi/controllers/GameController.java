package com.project.reversi.controllers;

import com.project.reversi.dto.BoardDTO;
import com.project.reversi.dto.MoveRequestDTO;
import com.project.reversi.dto.MoveResponseDTO;
import com.project.reversi.services.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/game")
public class GameController {

  private final GameService gameService;

  public GameController(GameService gameService) {
    this.gameService = gameService;
  }

  // Endpoint to retrieve the current board state.
  @GetMapping("/board")
  public BoardDTO getBoard() {
    return BoardDTO.fromBoard(gameService.getBoard());
  }

  // Endpoint to make a move.
  @PostMapping("/move")
  public ResponseEntity<MoveResponseDTO> makeMove(@RequestBody MoveRequestDTO moveRequest) {
    boolean valid = gameService.makeMove(moveRequest.getRow(), moveRequest.getColumn(), moveRequest.getColor());
    MoveResponseDTO response = new MoveResponseDTO();
    if (valid) {
      response.setMessage("Move successful");
    } else {
      response.setMessage("Invalid move");
    }
    response.setBoard(BoardDTO.fromBoard(gameService.getBoard()));
    return ResponseEntity.ok(response);
  }
}

