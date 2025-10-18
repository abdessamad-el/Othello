package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameState;
import com.project.reversi.model.MoveResult;
import com.project.reversi.model.Player;
import com.project.reversi.repository.JpaGameSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;

@Service
public class GameService {

  private final JpaGameSessionRepository sessionRepository;
  private final ComputerMoveEngine computerMoveEngine;
  private static final Logger logger = LoggerFactory.getLogger(GameService.class);

  public GameService(JpaGameSessionRepository sessionRepository, ComputerMoveEngine computerMoveEngine) {
    this.sessionRepository = sessionRepository;
    this.computerMoveEngine = computerMoveEngine;
  }

  /**
   * Processes a move for the given game session.
   * Validates that it is the correct player's turn, makes the move on the board,
   * advances the turn, and saves the updated session.
   *
   * @param sessionId   The ID of the game session.
   * @param row         The row for the move.
   * @param column      The column for the move.
   * @param playerColor The color of the player making the move.
   * @return The resut of the move
   */
  public MoveResult makeMove(String sessionId, int row, int column, Color playerColor, boolean passFlag, String seatToken) {
    GameSession session = sessionRepository.findById(sessionId).orElse(null);
    if (session == null) {
      logger.error("Session not found: {}", sessionId);
      throw new IllegalArgumentException("Session not found");
    }
    if (session.isFinished()) {
      logger.warn("Attempted move on finished session: {}", sessionId);
      return MoveResult.GAME_FINISHED;
    }

    if (seatToken == null || seatToken.isBlank()) {
      logger.error("Seat token missing for session {}", sessionId);
      return MoveResult.WRONG_TURN;
    }

    Player seatOwner = session.findPlayerBySeatToken(seatToken);
    if (seatOwner == null) {
      logger.error("Invalid seat token for session {}", sessionId);
      return MoveResult.WRONG_TURN;
    }

    if (!seatOwner.getColor().equals(playerColor)) {
      logger.error("Color mismatch for seat token in session {}", sessionId);
      return MoveResult.WRONG_TURN;
    }
    playerColor = seatOwner.getColor();

    if (session.getBoard().isGameOver()) {
      // Neither player can move; the game is over.
      logger.info("No valid moves for either player in session {}. Game is finished.", sessionId);
      finalizeGame(session);
      return MoveResult.GAME_FINISHED;
    }
    // Ensure it's the correct player's turn.
    Player currentPlayer = session.getCurrentPlayer();
    if (currentPlayer == null || !currentPlayer.equals(seatOwner)) {
      Color expectedColor = currentPlayer != null ? currentPlayer.getColor() : null;
      logger.error(
          "Wrong turn: move attempted by {} but current turn is for {}",
          playerColor,
          expectedColor
      );
      return MoveResult.WRONG_TURN;
    }
    // Check if the current player has any valid moves.
    if (!session.getBoard().hasValidMove(playerColor)) {
      logger.info("Player {} has no valid moves; passing turn.", playerColor);
      session.advanceTurn();
      session.snapshotBoard();
      computerMoveEngine.updateScores(session);
      sessionRepository.save(session);
      computerMoveEngine.playAll(session);
      computerMoveEngine.updateScores(session);
      session.snapshotBoard();
      sessionRepository.save(session);
      if (session.getBoard().isGameOver()) {
        finalizeGame(session);
        return MoveResult.GAME_FINISHED;
      }
      return MoveResult.PASS;
    }
    // If the pass flag is true, we don't expect a coordinate-based move.
    if (passFlag) {
      //If valid moves exist (even though pass was requested), that's an error.
      logger.error("Pass requested but valid moves exist for player {}", playerColor);
      return MoveResult.INVALID_PASS;
    }
    // Attempt the move on the board
    boolean moveResult = session.getBoard().makeMove(row, column, playerColor, false);
    if (moveResult) {
      logger.info("Player {} moved at ({}, {})", playerColor, row, column);
      session.advanceTurn();
      session.snapshotBoard();
      computerMoveEngine.updateScores(session);
      sessionRepository.save(session);
      if (session.getBoard().isGameOver()) {
        finalizeGame(session);
        return MoveResult.GAME_FINISHED;
      }

      computerMoveEngine.playAll(session);
      computerMoveEngine.updateScores(session);
      session.snapshotBoard();
      sessionRepository.save(session);
      if (session.getBoard().isGameOver()) {
        finalizeGame(session);
        return MoveResult.GAME_FINISHED;
      }
      return MoveResult.SUCCESS;

    } else {
      logger.info(
          "Invalid move attempted at ({}, {}) by player {}",
          row,
          column,
          playerColor.equals(Color.WHITE) ? "White" : "Black"
      );
      return MoveResult.INVALID_MOVE;
    }
  }

  public GameSession getSessionById(String sessionId) {
    return sessionRepository.findById(sessionId).orElse(null);
  }

  /**
   * Finalizes the game: calculates scores, sets the game state, and marks the session as finished.
   */
  private void finalizeGame(GameSession session) {
    int whiteCount = session.getBoard().getPieceCount(Color.WHITE);
    int blackCount = session.getBoard().getPieceCount(Color.BLACK);
    if (whiteCount > blackCount) {
      session.setGameState(GameState.WHITE_WINS);
    } else if (whiteCount < blackCount) {
      session.setGameState(GameState.BLACK_WINS);
    } else {
      session.setGameState(GameState.TIE);
    }
    session.setFinished(true);
    session.setWhiteScore(whiteCount);
    session.setBlackScore(blackCount);
    session.snapshotBoard();
    sessionRepository.save(session);
  }
}
