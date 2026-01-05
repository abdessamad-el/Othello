package com.project.reversi.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "game_session")
@EntityListeners(AuditingEntityListener.class)
public class GameSession {

  @Id
  private String sessionId;       // Unique identifier for the game session

  @Enumerated(EnumType.STRING)
  private GameType gameType;      // PLAYER_VS_PLAYER or PLAYER_VS_COMPUTER

  @Enumerated(EnumType.STRING)
  private GameState gameState;    // The state of the game


  @CreatedDate
  private LocalDateTime createdAt; // Timestamp of session creation

  @LastModifiedDate
  private LocalDateTime lastModifiedAt;

  private int currentTurnIndex;   // Index (0 or 1) for current player's turn
  private int whiteScore;         // piece count for white
  private int blackScore;         // piece count for black


  @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Player> players;   // Two players; for PVP, second can join later; for PVC, computer is auto-added.

  @Transient
  private Board board;
  // The game board
  @Column(name = "board_state", columnDefinition = "TEXT")
  private String boardState;

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
    this.currentTurnIndex = 0;
    this.board = board;
    this.boardState = BoardStateCodec.encode(board);
    this.players = new ArrayList<>(2);

    // Add the creator as Player 1 (seat 0).
    creator.setSeatIndex(0);
    creator.setSession(this);
    this.players.add(creator);

    if (gameType == GameType.PLAYER_VS_COMPUTER) {
      // Automatically create a computer player with the opposite color.
      PlayerColor computerColor = creator.getColor().opposite();
      Player computerPlayer = new Player(computerColor, true, "Computer", 1);
      computerPlayer.setSession(this);
      this.players.add(computerPlayer);
    }
    this.gameState = GameState.IN_PROGRESS;
    this.whiteScore = board.getPieceCount(PlayerColor.WHITE);
    this.blackScore = board.getPieceCount(PlayerColor.BLACK);
  }

  protected GameSession() {
    this.players = new ArrayList<>(2);
  }

  public String getSessionId() {
    return sessionId;
  }

  public Board getBoard() {
    return board;
  }

  public List<Player> getPlayers() {
    return players == null ? List.of() : players.stream()
                                                .sorted(Comparator.comparingInt(Player::getSeatIndex))
                                                .toList();
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

  public boolean isFinished() {
    return gameState != GameState.IN_PROGRESS;
  }

  public int getCurrentTurnIndex() {
    return currentTurnIndex;
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

  @Override
  public String toString() {
    return "GameSession{" +
           "sessionId='" + sessionId + '\'' +
           ", board=" + board +
           ", players=" + players +
           ", gameType=" + gameType +
           ", createdAt=" + createdAt +
           ", currentTurnIndex=" + currentTurnIndex +
           '}';
  }

  @PreUpdate
  protected void onUpdate() {
    this.boardState = BoardStateCodec.encode(board);
  }

  @PostLoad
  protected void postLoad() {
    board = BoardStateCodec.decode(boardState);
  }

  public boolean hasValidMove(PlayerColor color) {
    for (int i = 0; i < getBoard().getNumRows(); i++) {
      for (int j = 0; j < getBoard().getNumColumns(); j++) {
        // Use simulation mode (e.g., makeMove with simuMode=true) to check if the move would be valid
        List<Piece> result = getBoard().makeMove(i, j, color, true);
        if (!result.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  public List<int[]> computeValidMoves(PlayerColor color) {
    List<int[]> validMoves = new ArrayList<>();
    for (int row = 0; row < getBoard().getNumRows(); row++) {
      for (int col = 0; col < getBoard().getNumColumns(); col++) {
        List<Piece> result = getBoard().makeMove(row, col, color, true);
        if (!result.isEmpty()) {
          validMoves.add(new int[]{row, col});
        }
      }
    }
    return validMoves;
  }


  public boolean isGameOver() {
    return getBoard().getPieceCount(PlayerColor.WHITE) + getBoard().getPieceCount(PlayerColor.BLACK) == 64
           || (!hasValidMove(PlayerColor.WHITE) && !hasValidMove(PlayerColor.BLACK));
  }

}
