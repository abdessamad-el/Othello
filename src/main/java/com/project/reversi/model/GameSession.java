package com.project.reversi.model;

import javax.persistence.*;
import java.awt.Color;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "game_session")
public class GameSession {

  @Id
  private String sessionId;       // Unique identifier for the game session

  @Enumerated(EnumType.STRING)
  private GameType gameType;      // PLAYER_VS_PLAYER or PLAYER_VS_COMPUTER

  @Enumerated(EnumType.STRING)
  private GameState gameState;    // The state of the game

  private LocalDateTime createdAt;// Timestamp of session creation
  private LocalDateTime lastModifiedAt;

  private boolean finished;       // Whether the session is finished
  private int currentTurnIndex;   // Index (0 or 1) for current player's turn
  private int whiteScore;         // piece count for white
  private int blackScore;         // piece count for black

  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Player> players = new ArrayList<>();   // Two players; for PVP, second can join later; for PVC, computer is auto-added.

  @Transient
  private Board board;            // The game board

  @Lob
  @Column(name = "board_state")
  private String boardState;      // JSON representation

  @Version
  private Integer version;

  /**
   * Creates a new game session.
   * For PLAYER_VS_COMPUTER, the computer player is automatically added.
   * For PLAYER_VS_PLAYER, a placeholder (null) is added for player 2.
   */
  public GameSession(Board board, Player creator, GameType gameType) {
    this.sessionId = UUID.randomUUID().toString();
    this.gameType = gameType;
    this.createdAt = LocalDateTime.now();
    this.lastModifiedAt = this.createdAt;
    this.finished = false;
    this.currentTurnIndex = 0;
    this.board = board;
    snapshotBoard();

    this.players = new ArrayList<>(2);
    
    // Add the creator as Player 1 (seat 0).
    creator.setSeatIndex(0);
    creator.setSession(this);
    this.players.add(creator);

    if (gameType == GameType.PLAYER_VS_COMPUTER) {
      // Automatically create a computer player with the opposite color.
      Color computerColor = creator.getColor().equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
      Player computerPlayer = new Player(computerColor, true, "Computer", 1);
      computerPlayer.setSession(this);
      this.players.add(computerPlayer);
    } else {
      // placeholder seat for Player 2
    }
    this.gameState = GameState.IN_PROGRESS;
    this.whiteScore = board.getPieceCount(Color.WHITE);
    this.blackScore = board.getPieceCount(Color.BLACK);
  }

  protected GameSession() {
    this.players = new ArrayList<>(2);
  }

  public String getSessionId() {
    return sessionId;
  }

  public Board getBoard() {
    if (board == null && boardState != null) {
      board = BoardStateCodec.decode(boardState);
      if (board == null) {
        board = new Board(8, 8);
      }
    }
    return board;
  }

  public void setBoard(Board board) {
    this.board = board;
    snapshotBoard();
  }

  public List<Player> getPlayers() {
    return players == null ? List.of() : players.stream()
                                                   .sorted(Comparator.comparingInt(Player::getSeatIndex))
                                                   .collect(Collectors.toUnmodifiableList());
  }

  public List<Player> getPlayersWithPlaceholders() {
    List<Player> ordered = new ArrayList<>(2);
    ordered.add(getPlayerAtSeat(0));
    ordered.add(getPlayerAtSeat(1));
    return ordered;
  }

  public Player getPlayerAtSeat(int seatIndex) {
    if (players == null) {
      return null;
    }
    return players.stream().filter(p -> p.getSeatIndex() == seatIndex).findFirst().orElse(null);
  }

  public GameType getGameType() {
    return gameType;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getLastModifiedAt() {
    return lastModifiedAt;
  }

  public Integer getVersion() {
    return version;
  }

  public String getBoardState() {
    return boardState;
  }

  public void setBoardState(String boardState) {
    this.boardState = boardState;
    this.board = BoardStateCodec.decode(boardState);
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
    return getPlayerAtSeat(currentTurnIndex);
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
  }

  /**
   * For PLAYER_VS_PLAYER sessions, allows a second player to join.
   * Throws an exception if a second player is already present or if the game type is not PLAYER_VS_PLAYER.
   */
  public void joinSession(Player player) {
    if (gameType != GameType.PLAYER_VS_PLAYER) {
      throw new IllegalStateException("Cannot join session in non-player vs. player mode");
    }
    Player seatOne = getPlayerAtSeat(1);
    if (seatOne != null) {
      throw new IllegalStateException("Session already has two players.");
    }
    player.setSeatIndex(1);
    player.setSession(this);
    if (players == null) {
      players = new ArrayList<>(2);
    }
    players.add(player);
  }

  // Returns true if the session is ready to start (i.e., has two non-null players).
  public boolean isReady() {
    if (gameType == GameType.PLAYER_VS_COMPUTER) {
      return true;
    }
    return getPlayerAtSeat(0) != null && getPlayerAtSeat(1) != null;
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

  @PrePersist
  protected void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    lastModifiedAt = LocalDateTime.now();
    snapshotBoard();
  }

  @PreUpdate
  protected void onUpdate() {
    lastModifiedAt = LocalDateTime.now();
    snapshotBoard();
  }

  public void snapshotBoard() {
    this.boardState = BoardStateCodec.encode(board);
  }
}
