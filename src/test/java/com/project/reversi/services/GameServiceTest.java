package com.project.reversi.services;

import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameState;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MoveResult;
import com.project.reversi.model.Piece;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.repository.JpaGameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class GameServiceTest {

  @Autowired
  private JpaGameSessionRepository repository;

  private GameService gameService;
  private GameSessionService gameSessionService;
  private ComputerMoveEngine computerMoveEngine;

  @BeforeEach
  void setup() {
    computerMoveEngine = new ComputerMoveEngine();
    gameService = new GameService(repository, computerMoveEngine);
    gameSessionService = new GameSessionService(repository);
  }

  @Test
  void gameFinishedWhenBoardIsOver() {
    Board board = new FakeBoardGameOver();
    GameSession session = new GameSession(board, new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, PlayerColor.WHITE);
    assertEquals(MoveResult.GAME_FINISHED, result);

    GameSession saved = repository.findById(session.getSessionId()).orElseThrow();
    assertTrue(saved.isFinished());
    assertEquals(4, saved.getWhiteScore());
    assertEquals(6, saved.getBlackScore());
    assertEquals(GameState.BLACK_WINS, saved.getGameState());
  }

  @Test
  void wrongTurnReturnsError() {
    // Create a normal session where it's WHITE's turn (index 0)
    GameSession session = gameSessionService.createGameSession(GameType.PLAYER_VS_PLAYER, new Player(PlayerColor.WHITE));
    // Attempt a move by BLACK on WHITE's turn
    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, PlayerColor.BLACK);
    assertEquals(MoveResult.WRONG_TURN, result);
  }

  @Test
  void invalidMoveWhenHasValidMoves() {
    Board board = new FakeBoardInvalidMove();
    GameSession session = new GameSession(board, new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 2, 2, PlayerColor.WHITE);
    assertEquals(MoveResult.INVALID_MOVE, result);
  }

  @Test
  void successfulMoveAdvancesTurnAndUpdatesScore() {
    Board board = new FakeBoardValidMove();
    GameSession session = new GameSession(board, new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 3, 3, PlayerColor.WHITE);
    assertEquals(MoveResult.SUCCESS, result);

    GameSession saved = repository.findById(session.getSessionId()).orElseThrow();
    assertEquals(1, saved.getCurrentTurnIndex()); // turn should advance to player 2
    assertEquals(5, saved.getWhiteScore());
    assertEquals(4, saved.getBlackScore());
    assertEquals(GameState.IN_PROGRESS, saved.getGameState());
  }

  @Test
  void successfulMoveSkipsOpponentWithoutMoves() {
    Board board = new FakeBoardNextPlayerNoMoves();
    GameSession session = new GameSession(board, new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    session.joinSession(new Player(PlayerColor.BLACK));
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 3, 3, PlayerColor.WHITE);
    assertEquals(MoveResult.SUCCESS, result);

    GameSession saved = repository.findById(session.getSessionId()).orElseThrow();
    assertEquals(0, saved.getCurrentTurnIndex());
  }

  @Test
  void gameFinishedWhiteWins() {
    Board board = new FakeBoardGameOverWhiteWins();
    GameSession session = new GameSession(board, new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, PlayerColor.WHITE);
    assertEquals(MoveResult.GAME_FINISHED, result);

    GameSession saved = repository.findById(session.getSessionId()).orElseThrow();
    assertTrue(saved.isFinished());
    assertEquals(7, saved.getWhiteScore());
    assertEquals(2, saved.getBlackScore());
    assertEquals(GameState.WHITE_WINS, saved.getGameState());
  }

  @Test
  void gameFinishedTie() {
    Board board = new FakeBoardGameOverTie();
    GameSession session = new GameSession(board, new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, PlayerColor.WHITE);
    assertEquals(MoveResult.GAME_FINISHED, result);

    GameSession saved = repository.findById(session.getSessionId()).orElseThrow();
    assertTrue(saved.isFinished());
    assertEquals(5, saved.getWhiteScore());
    assertEquals(5, saved.getBlackScore());
    assertEquals(GameState.TIE, saved.getGameState());
  }

  /**
   * Fake board that simulates game over with BLACK leading.
   */
  static class FakeBoardGameOver extends Board {
    FakeBoardGameOver() { super(8, 8); }
    @Override
    public List<Piece> makeMove(int row, int column, PlayerColor color, boolean simuMode) { return Collections.emptyList(); }
    @Override
    public int getPieceCount(PlayerColor color) { return color == PlayerColor.WHITE ? 4 : 6; }
  }

  /**
   * Fake board where hasValidMove=true but makeMove returns false
   */
  static class FakeBoardInvalidMove extends Board {
    FakeBoardInvalidMove() { super(8, 8); }
    @Override
    public List<Piece> makeMove(int row, int column, PlayerColor color, boolean simuMode) {
      if (simuMode) {
        return Collections.singletonList(new Piece(PlayerColor.WHITE));
      }
      return Collections.emptyList();
    }
    @Override
    public int getPieceCount(PlayerColor color) { return 2; }
  }

  /**
   * Fake board where a move succeeds and scores increase for WHITE.
   */
  static class FakeBoardValidMove extends Board {
    FakeBoardValidMove() { super(8, 8); }
    @Override
    public List<Piece> makeMove(int row, int column, PlayerColor color, boolean simuMode) { return Collections.singletonList(new Piece(PlayerColor.WHITE)); }
    @Override
    public int getPieceCount(PlayerColor color) { return color == PlayerColor.WHITE ? 5 : 4; }
  }

  /**
   * Fake board where WHITE can move, BLACK cannot.
   */
  static class FakeBoardNextPlayerNoMoves extends Board {
    FakeBoardNextPlayerNoMoves() { super(8, 8); }
    @Override
    public List<Piece> makeMove(int row, int column, PlayerColor color, boolean simuMode) {
      if (color == PlayerColor.BLACK) {
        return Collections.emptyList();
      }
      return Collections.singletonList(new Piece(PlayerColor.WHITE));
    }
    @Override
    public int getPieceCount(PlayerColor color) { return color == PlayerColor.WHITE ? 5 : 4; }
  }

  /**
   * Fake board where game over and WHITE leads.
   */
  static class FakeBoardGameOverWhiteWins extends Board {
    FakeBoardGameOverWhiteWins() { super(8, 8); }
    @Override
    public List<Piece> makeMove(int row, int column, PlayerColor color, boolean simuMode) { return Collections.emptyList(); }
    @Override
    public int getPieceCount(PlayerColor color) { return color == PlayerColor.WHITE ? 7 : 2; }
  }

  /**
   * Fake board where game over and tie.
   */
  static class FakeBoardGameOverTie extends Board {
    FakeBoardGameOverTie() { super(8, 8); }
    @Override
    public List<Piece> makeMove(int row, int column, PlayerColor color, boolean simuMode) { return Collections.emptyList(); }
    @Override
    public int getPieceCount(PlayerColor color) { return 5; }
  }
}

