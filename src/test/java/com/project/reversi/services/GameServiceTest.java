package com.project.reversi.services;

import com.project.reversi.model.*;
import com.project.reversi.repository.InMemoryGameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameServiceTest {

  private InMemoryGameSessionRepository repository;
  private GameService gameService;
  private GameSessionService gameSessionService;

  @BeforeEach
  void setup() {
    repository = new InMemoryGameSessionRepository();
    gameService = new GameService(repository);
    gameSessionService = new GameSessionService(repository);
  }

  @Test
  void invalidPassWhenValidMovesExist() {
    GameSession session = gameSessionService.createGameSession(GameType.PLAYER_VS_PLAYER, new Player(Color.WHITE));

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, Color.WHITE, true);
    assertEquals(MoveResult.INVALID_PASS, result);
  }

  @Test
  void passWhenNoValidMoves() {
    Board board = new FakeBoardNoMoves();
    GameSession session = new GameSession(board, new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, Color.WHITE, false);
    assertEquals(MoveResult.PASS, result);

    GameSession saved = repository.findById(session.getSessionId());
    assertEquals(2, saved.getWhiteScore());
    assertEquals(3, saved.getBlackScore());
    assertEquals(GameState.IN_PROGRESS, saved.getGameState());
    assertTrue(!saved.isFinished());
  }

  @Test
  void gameFinishedWhenBoardIsOver() {
    Board board = new FakeBoardGameOver();
    GameSession session = new GameSession(board, new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, Color.WHITE, false);
    assertEquals(MoveResult.GAME_FINISHED, result);

    GameSession saved = repository.findById(session.getSessionId());
    assertTrue(saved.isFinished());
    assertEquals(4, saved.getWhiteScore());
    assertEquals(6, saved.getBlackScore());
    assertEquals(GameState.BLACK_WINS, saved.getGameState());
  }

  @Test
  void wrongTurnReturnsError() {
    // Create a normal session where it's WHITE's turn (index 0)
    GameSession session = gameSessionService.createGameSession(GameType.PLAYER_VS_PLAYER, new Player(Color.WHITE));
    // Attempt a move by BLACK on WHITE's turn
    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, Color.BLACK, false);
    assertEquals(MoveResult.WRONG_TURN, result);
  }

  @Test
  void invalidMoveWhenHasValidMoves() {
    Board board = new FakeBoardInvalidMove();
    GameSession session = new GameSession(board, new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 2, 2, Color.WHITE, false);
    assertEquals(MoveResult.INVALID_MOVE, result);
  }

  @Test
  void successfulMoveAdvancesTurnAndUpdatesScore() {
    Board board = new FakeBoardValidMove();
    GameSession session = new GameSession(board, new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 3, 3, Color.WHITE, false);
    assertEquals(MoveResult.SUCCESS, result);

    GameSession saved = repository.findById(session.getSessionId());
    assertEquals(1, saved.getCurrentTurnIndex()); // turn should advance to player 2
    assertEquals(5, saved.getWhiteScore());
    assertEquals(4, saved.getBlackScore());
    assertEquals(GameState.IN_PROGRESS, saved.getGameState());
  }

  @Test
  void gameFinishedWhiteWins() {
    Board board = new FakeBoardGameOverWhiteWins();
    GameSession session = new GameSession(board, new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, Color.WHITE, false);
    assertEquals(MoveResult.GAME_FINISHED, result);

    GameSession saved = repository.findById(session.getSessionId());
    assertTrue(saved.isFinished());
    assertEquals(7, saved.getWhiteScore());
    assertEquals(2, saved.getBlackScore());
    assertEquals(GameState.WHITE_WINS, saved.getGameState());
  }

  @Test
  void gameFinishedTie() {
    Board board = new FakeBoardGameOverTie();
    GameSession session = new GameSession(board, new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    repository.save(session);

    MoveResult result = gameService.makeMove(session.getSessionId(), 0, 0, Color.WHITE, false);
    assertEquals(MoveResult.GAME_FINISHED, result);

    GameSession saved = repository.findById(session.getSessionId());
    assertTrue(saved.isFinished());
    assertEquals(5, saved.getWhiteScore());
    assertEquals(5, saved.getBlackScore());
    assertEquals(GameState.TIE, saved.getGameState());
  }

  /**
   * Fake board that simulates no valid moves for any player and not game over.
   */
  static class FakeBoardNoMoves extends Board {
    FakeBoardNoMoves() { super(8, 8); }
    @Override
    public boolean hasValidMove(Color color) { return false; }
    @Override
    public boolean isGameOver() { return false; }
    @Override
    public int getPieceCount(Color color) { return color.equals(Color.WHITE) ? 2 : 3; }
  }

  /**
   * Fake board that simulates game over with BLACK leading.
   */
  static class FakeBoardGameOver extends Board {
    FakeBoardGameOver() { super(8, 8); }
    @Override
    public boolean isGameOver() { return true; }
    @Override
    public boolean hasValidMove(Color color) { return false; }
    @Override
    public int getPieceCount(Color color) { return color.equals(Color.WHITE) ? 4 : 6; }
  }

  /**
   * Fake board where hasValidMove=true but makeMove returns false
   */
  static class FakeBoardInvalidMove extends Board {
    FakeBoardInvalidMove() { super(8, 8); }
    @Override
    public boolean hasValidMove(Color color) { return true; }
    @Override
    public boolean makeMove(int row, int column, Color color, boolean simuMode) { return false; }
    @Override
    public boolean isGameOver() { return false; }
    @Override
    public int getPieceCount(Color color) { return color.equals(Color.WHITE) ? 2 : 2; }
  }

  /**
   * Fake board where a move succeeds and scores increase for WHITE.
   */
  static class FakeBoardValidMove extends Board {
    FakeBoardValidMove() { super(8, 8); }
    @Override
    public boolean hasValidMove(Color color) { return true; }
    @Override
    public boolean makeMove(int row, int column, Color color, boolean simuMode) { return true; }
    @Override
    public boolean isGameOver() { return false; }
    @Override
    public int getPieceCount(Color color) { return color.equals(Color.WHITE) ? 5 : 4; }
  }

  /**
   * Fake board where game over and WHITE leads.
   */
  static class FakeBoardGameOverWhiteWins extends Board {
    FakeBoardGameOverWhiteWins() { super(8, 8); }
    @Override
    public boolean isGameOver() { return true; }
    @Override
    public boolean hasValidMove(Color color) { return false; }
    @Override
    public int getPieceCount(Color color) { return color.equals(Color.WHITE) ? 7 : 2; }
  }

  /**
   * Fake board where game over and tie.
   */
  static class FakeBoardGameOverTie extends Board {
    FakeBoardGameOverTie() { super(8, 8); }
    @Override
    public boolean isGameOver() { return true; }
    @Override
    public boolean hasValidMove(Color color) { return false; }
    @Override
    public int getPieceCount(Color color) { return 5; }
  }
}

