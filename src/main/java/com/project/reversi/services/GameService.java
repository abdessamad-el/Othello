package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.MoveResult;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.repository.JpaGameSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

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
  public MoveResult makeMove(String sessionId, int row, int column, PlayerColor playerColor) {
    GameSession session = sessionRepository.findById(sessionId)
                                           .orElseThrow(() -> new NoSuchElementException("Session not found: "
                                                                                         + sessionId));
    if (session.isFinished()) {
      logger.warn("Attempted move on finished session: {}", sessionId);
      return MoveResult.GAME_FINISHED;
    }
    // Ensure it's the correct player's turn.
    Player currentPlayer = session.getCurrentPlayer();
    if (currentPlayer.getColor() != playerColor) {
      logger.error(
          "Wrong turn: move attempted by {} but current turn is for {}",
          playerColor,
          currentPlayer.getColor()
      );
      return MoveResult.WRONG_TURN;
    }
    // Attempt the move on the board
    boolean moveResult = session.getBoard().makeMove(row, column, playerColor);

    if (!moveResult) {
      logger.info(
          "Invalid move attempted at ({}, {}) by player {}",
          row,
          column,
          playerColor == PlayerColor.WHITE ? "White" : "Black"
      );
      return MoveResult.INVALID_MOVE;
    }
    logger.info("Player {} moved at ({}, {})", playerColor, row, column);

    // Next turn + pass logic
    session.advanceTurnWithPass();

    if (session.isGameOver()) {
      session.finish();
      return MoveResult.GAME_FINISHED;
    }

    // Let computer play while it’s computer turn
    while (!session.isFinished() && session.getCurrentPlayer() != null && session.getCurrentPlayer().isComputer()) {
      boolean acted = computerMoveEngine.playSingleTurn(session);
      if (!acted) break;

      if (session.isGameOver()) {
        session.finish();
        return MoveResult.GAME_FINISHED;
      }
    }

    session.updateScores();
    sessionRepository.save(session);
    return MoveResult.SUCCESS;

  }

  public GameSession getSessionById(String sessionId) {
    return sessionRepository.findById(sessionId).orElse(null);
  }
}
