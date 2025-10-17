package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;

@Service
public class ComputerMoveEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputerMoveEngine.class);

  /**
   * Execute computer turns until it is no longer the computer's move.
   */
  public void playAll(GameSession session) {
    boolean acted;
    do {
      acted = playSingleTurn(session);
    } while (acted && isComputerTurn(session));
  }

  private boolean playSingleTurn(GameSession session) {
    if (!isComputerTurn(session)) {
      return false;
    }

    Player computer = session.getCurrentPlayer();
    Color computerColor = computer.getColor();

    if (!session.getBoard().hasValidMove(computerColor)) {
      LOGGER.info("Computer has no valid moves; passing turn.");
      session.advanceTurn();
      return true;
    }

    boolean moveMade = false;
    for (int row = 0; row < session.getBoard().getNumRows() && !moveMade; row++) {
      for (int col = 0; col < session.getBoard().getNumColumns() && !moveMade; col++) {
        if (session.getBoard().makeMove(row, col, computerColor, false)) {
          LOGGER.info("Computer moved at ({}, {})", row, col);
          moveMade = true;
        }
      }
    }

    if (!moveMade) {
      // Should not happen because we already checked hasValidMove, but guard anyway
      session.advanceTurn();
      return true;
    }

    session.snapshotBoard();
    updateScores(session);
    session.advanceTurn();
    return true;
  }

  private boolean isComputerTurn(GameSession session) {
    Player current = session.getCurrentPlayer();
    return current != null && current.isComputer();
  }

  public void updateScores(GameSession session) {
    session.setBlackScore(session.getBoard().getPieceCount(Color.BLACK));
    session.setWhiteScore(session.getBoard().getPieceCount(Color.WHITE));
  }
}

