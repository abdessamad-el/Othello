package com.project.reversi.dto;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.Player;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class GameSessionSummaryDTO {
  private String sessionId;
  private BoardDTO board;  // Use BoardDTO to represent the board state
  private List<String> playerColors; // e.g., "WHITE" or "BLACK" (or "N/A" if not present)
  private String currentPlayerColor;
  private boolean finished;
  private String gameType;
  private int whiteScore;
  private int blackScore;
  private String gameState;

  // Getters and setters

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public BoardDTO getBoard() {
    return board;
  }

  public void setBoard(BoardDTO board) {
    this.board = board;
  }

  public List<String> getPlayerColors() {
    return playerColors;
  }

  public void setPlayerColors(List<String> playerColors) {
    this.playerColors = playerColors;
  }

  public String getCurrentPlayerColor() {
    return currentPlayerColor;
  }

  public void setCurrentPlayerColor(String currentPlayerColor) {
    this.currentPlayerColor = currentPlayerColor;
  }

  public boolean isFinished() {
    return finished;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public String getGameType() {
    return gameType;
  }

  public void setGameType(String gameType) {
    this.gameType = gameType;
  }

  public int getBlackScore() {
    return blackScore;
  }

  public int getWhiteScore() {
    return whiteScore;
  }

  public String getGameState() {
    return gameState;
  }

  public void setBlackScore(int pieceCount) {
    blackScore = pieceCount;
  }

  public void setWhiteScore(int pieceCount) {
    whiteScore = pieceCount;
  }

  public void setGameState(String name) {
    gameState = name;
  }

  public static GameSessionSummaryDTO fromGameSession(GameSession session) {
    GameSessionSummaryDTO summary = new GameSessionSummaryDTO();
    summary.setSessionId(session.getSessionId());
    // Use BoardDTO to provide a structured view of the board
    summary.setBoard(BoardDTO.fromBoard(session.getBoard()));

    // Map players to their color strings. If a player is null, use "N/A".
    summary.setPlayerColors(
        session.getPlayers().stream()
               .map(player -> player == null ? "N/A" : player.getColor().equals(Color.BLACK) ? "BLACK" : "WHITE")
               .collect(Collectors.toList())
    );

    Player currentPlayer = session.getCurrentPlayer();
    summary.setCurrentPlayerColor(currentPlayer != null ? currentPlayer.getColor().equals(Color.BLACK)
                                                          ? "BLACK"
                                                          : "WHITE" : "N/A");
    summary.setFinished(session.isFinished());
    summary.setGameType(session.getGameType().name());
    summary.setGameState(session.getGameState().name());
    summary.setWhiteScore(session.getWhiteScore());
    summary.setBlackScore(session.getBlackScore());
    return summary;
  }
}

