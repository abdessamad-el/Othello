package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ComputerMoveEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputerMoveEngine.class);


  private final ComputerStrategy strategy;

  public ComputerMoveEngine(ComputerStrategy strategy) {this.strategy = strategy;}


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

    if(session.isFinished()){
      return false;
    }

    Player computer = session.getCurrentPlayer();
    PlayerColor computerColor = computer.getColor();

    if (!session.hasValidMove(computerColor)) {
      LOGGER.info("Computer has no valid moves; passing turn.");
      session.advanceTurn();
      return true;
    }
    
    Position move = strategy.execute(session, computerColor);
    if (move.row() == -1 && move.col() == -1){
      LOGGER.info("Computer has no valid moves; passing turn.");
      session.advanceTurn();
      return true;
    }
    boolean result = session.getBoard().makeMove(move.row(), move.col(), computerColor);
    if(!result){
      LOGGER.error("Computer strategy returned an invalid move at ({}, {})", move.row(), move.col());
      return false;
    }
    LOGGER.info("Computer {} moved at ({}, {})",computerColor, move.row(), move.col());
    updateScores(session);
    session.advanceTurn();
    // advance turn if human player has no valid moves
    if(session.getCurrentPlayer() != null && !session.hasValidMove(session.getCurrentPlayer().getColor())){
      session.advanceTurn();
    }
    return true;
  }

  private boolean isComputerTurn(GameSession session) {
    Player current = session.getCurrentPlayer();
    return current != null && current.isComputer();
  }

  public void updateScores(GameSession session) {
    session.setBlackScore(session.getBoard().getPieceCount(PlayerColor.BLACK));
    session.setWhiteScore(session.getBoard().getPieceCount(PlayerColor.WHITE));
  }
}

