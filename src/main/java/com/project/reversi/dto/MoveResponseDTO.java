package com.project.reversi.dto;

public class MoveResponseDTO {
  private String message;
  private BoardDTO board;

  // Getters and setters
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public BoardDTO getBoard() {
    return board;
  }

  public void setBoard(BoardDTO board) {
    this.board = board;
  }
}

