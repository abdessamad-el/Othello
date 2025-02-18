package com.project.reversi.dto;

public class MoveResponseDTO {
  private String message;
  private GameSessionSummaryDTO sessionSummary;

  // Getters and setters
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public GameSessionSummaryDTO getSessionSummary() {
    return sessionSummary;
  }

  public void setSessionSummary(GameSessionSummaryDTO sessionSummary) {
    this.sessionSummary = sessionSummary;
  }
}

