package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameState;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MoveResult;
import com.project.reversi.model.Player;
import com.project.reversi.repository.GameSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;

@Service
public class GameService {

  private final GameSessionRepository sessionRepository;
  private static final Logger logger = LoggerFactory.getLogger(GameService.class);

  public GameService(GameSessionRepository sessionRepository) {
    this.sessionRepository = sessionRepository;
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
  public MoveResult makeMove(String sessionId, int row, int column, Color playerColor, boolean passFlag) {
    GameSession session = sessionRepository.findById(sessionId);
    if (session == null) {
      logger.error("Session not found: {}", sessionId);
      throw new IllegalArgumentException("Session not found");
    }
    if (session.isFinished()) {
      logger.warn("Attempted move on finished session: {}", sessionId);
      return MoveResult.GAME_FINISHED;
    }
    // Ensure it's the correct player's turn.
    Player currentPlayer = session.getCurrentPlayer();
    if (!currentPlayer.getColor().equals(playerColor)) {
      logger.error(
          "Wrong turn: move attempted by {} but current turn is for {}",
          playerColor,
          currentPlayer.getColor()
      );
      return MoveResult.WRONG_TURN;
    }
    // Check if the current player has any valid moves.
    if (!session.getBoard().hasValidMove(playerColor)) {
      // Check if the opponent has any valid moves.
      Color otherPlayerColor = session.getBoard().getOppositeColor(playerColor);
      if (session.getBoard().hasValidMove(otherPlayerColor)) {
        logger.info("Player {} has no valid moves; passing turn.", playerColor);
        session.advanceTurn();
        sessionRepository.save(session);
        // In PLAYER_VS_COMPUTER mode, if it is now the computer's turn, perform the computer move.
        if (session.getGameType() == GameType.PLAYER_VS_COMPUTER &&
            session.getCurrentPlayer().isComputer()) {
          performComputerMove(session);
          return MoveResult.COMPUTER_MOVED;
        }
        return MoveResult.NO_MOVES_AVAILABLE;
      } else {
        // Neither player can move; the game is over.
        logger.info("No valid moves for either player in session {}. Game is finished.", sessionId);
        finalizeGame(session);
        return MoveResult.GAME_FINISHED;
      }
    }
    // If the pass flag is true, we don't expect a coordinate-based move.
    if (Boolean.TRUE.equals(passFlag)) {
      //If valid moves exist (even though pass was requested), that's an error.
      logger.error("Pass requested but valid moves exist for player {}", playerColor);
      return MoveResult.INVALID_PASS;
    }
    // Attempt the move on the board
    boolean moveResult = session.getBoard().makeMove(row, column, playerColor, false);
    if (moveResult) {
      session.advanceTurn();
      updateScore(session);
      sessionRepository.save(session);
      // Only perform the computer move if the game type is PLAYER_VS_COMPUTER
      // and if it's now the computer's turn.
      if (session.getGameType() == GameType.PLAYER_VS_COMPUTER &&
          session.getCurrentPlayer().isComputer()) {
        performComputerMove(session);
      }
      // check if the game is finished and finalize the session
      if (session.getBoard().isGameOver()) {
        finalizeGame(session);
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

  private static void updateScore(GameSession session) {
    session.setBlackScore(session.getBoard().getPieceCount(Color.BLACK));
    session.setWhiteScore(session.getBoard().getPieceCount(Color.WHITE));
  }

  public GameSession getSessionById(String sessionId) {
    return sessionRepository.findById(sessionId);
  }

  private void performComputerMove(GameSession session) {
    Color computerColor = session.getCurrentPlayer().getColor();

    // Check if the computer has any valid moves.
    if (!session.getBoard().hasValidMove(computerColor)) {
      logger.info("Computer has no valid moves; passing turn.");
      session.advanceTurn();
      sessionRepository.save(session);
      return;
    }

    // Otherwise, attempt to find a valid move.
    boolean moveMade = false;
    for (int i = 0; i < session.getBoard().getNumRows() && !moveMade; i++) {
      for (int j = 0; j < session.getBoard().getNumColumns() && !moveMade; j++) {
        if (session.getBoard().makeMove(i, j, computerColor, false)) {
          logger.info("Computer moved at ({}, {})", i, j);
          moveMade = true;
        }
      }
    }

    // advance the turn and save the session.
    session.advanceTurn();
    // update score
    updateScore(session);
    sessionRepository.save(session);
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
    sessionRepository.save(session);
  }
}
