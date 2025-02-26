package com.project.reversi.model;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GameSession {

  private String sessionId;       // Unique identifier for the game session
  private Board board;            // The game board
  private List<Player> players;   // Two players; for PVP, second can join later; for PVC, computer is auto-added.
  private GameType gameType;      // PLAYER_VS_PLAYER or PLAYER_VS_COMPUTER
  private LocalDateTime createdAt;// Timestamp of session creation
  private boolean finished;       // Whether the session is finished
  private int currentTurnIndex;   // Index (0 or 1) for current player's turn
  private GameState gameState;    // The state of the game
  private int whiteScore;         // piece count for white
  private int blackScore;         // piece count for black
  private final List<Runnable> procedures = new ArrayList<>();

  /**
   * Creates a new game session.
   * For PLAYER_VS_COMPUTER, the computer player is automatically added.
   * For PLAYER_VS_PLAYER, a placeholder (null) is added for player 2.
   */
  public GameSession(Board board, Player creator, GameType gameType) {
    this.sessionId = UUID.randomUUID().toString();
    this.board = board;
    this.gameType = gameType;
    this.createdAt = LocalDateTime.now();
    this.finished = false;
    this.currentTurnIndex = 0;
    this.players = new ArrayList<>();
    // Add the creator as Player 1.
    this.players.add(creator);

    if (gameType == GameType.PLAYER_VS_COMPUTER) {
      // Automatically create a computer player with the opposite color.
      Color computerColor = creator.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
      Player computerPlayer = new Player(computerColor, true,this);
      this.players.add(computerPlayer);
    } else {
      // For player vs. player, add a placeholder for Player 2.
      this.players.add(null);
    }
    this.gameState = GameState.IN_PROGRESS;
    this.whiteScore = board.getPieceCount(Color.WHITE);
    this.blackScore = board.getPieceCount(Color.BLACK);
  }

  public String getSessionId() {
    return sessionId;
  }

  public Board getBoard() {
    return board;
  }

  public void setBoard(Board board) {
    this.board = board;
  }

  public List<Player> getPlayers() {
    return players;
  }

  public GameType getGameType() {
    return gameType;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public boolean isFinished() {
    return finished;
  }

  public void setFinished(boolean finished) {
    this.finished = finished;
  }

  public int getCurrentTurnIndex() {
    return currentTurnIndex;
  }

  public void setCurrentTurnIndex(int currentTurnIndex) {
    this.currentTurnIndex = currentTurnIndex;
  }

  // Returns the player whose turn it is.
  public Player getCurrentPlayer() {
    return players.get(currentTurnIndex);
  }

  public GameState getGameState() {
    return gameState;
  }

  public void setGameState(GameState gameResult) {
    this.gameState = gameResult;
  }

  public int getWhiteScore() {
    return whiteScore;
  }

  public void setWhiteScore(int whiteScore) {
    this.whiteScore = whiteScore;
  }

  public int getBlackScore() {
    return blackScore;
  }

  public void setBlackScore(int blackScore) {
    this.blackScore = blackScore;
  }

  // Advances the turn (for two players, cycles between 0 and 1).
  public void advanceTurn() {
    currentTurnIndex = (currentTurnIndex + 1) % 2;
    if (currentTurnIndex % 2 == 1 && gameType == GameType.PLAYER_VS_COMPUTER){
      // notify the computer to play if it's turn
      for (Runnable procedure : procedures) {
        procedure.run();
      }
    }
  }

  /**
   * For PLAYER_VS_PLAYER sessions, allows a second player to join.
   * Throws an exception if a second player is already present or if the game type is not PLAYER_VS_PLAYER.
   */
  public void joinSession(Player player) {
    if (gameType != GameType.PLAYER_VS_PLAYER) {
      throw new IllegalStateException("Cannot join session in non-player vs. player mode");
    }
    if (players.get(1) != null) {
      throw new IllegalStateException("Session already has two players.");
    }
    players.set(1, player);
  }

  // Returns true if the session is ready to start (i.e., has two non-null players).
  public boolean isReady() {
    if (gameType == GameType.PLAYER_VS_COMPUTER) {
      return true;
    }
    return players.get(0) != null && players.get(1) != null;
  }

  @Override
  public String toString() {
    return "GameSession{" +
           "sessionId='" + sessionId + '\'' +
           ", board=" + board +
           ", players=" + players +
           ", gameType=" + gameType +
           ", createdAt=" + createdAt +
           ", finished=" + finished +
           ", currentTurnIndex=" + currentTurnIndex +
           '}';
  }

  public List<Runnable> OnChanged() {
    return procedures;
  }
}
