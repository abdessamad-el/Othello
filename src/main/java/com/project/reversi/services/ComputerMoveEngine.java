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



  public boolean playSingleTurn(GameSession session) {

    if(session.isFinished()){
      return false;
    }

    if (!isComputerTurn(session)) {
      return false;
    }
    Player computer = session.getCurrentPlayer();
    PlayerColor computerColor = computer.getColor();

    if (!session.hasValidMove(computerColor)) {
      LOGGER.info("Computer has no valid moves; passing turn.");
      session.advanceTurnWithPass();
      return true;
    }
    
    Position move = strategy.execute(session, computerColor);
    if (move.row() == -1){
      LOGGER.info("Computer has no valid moves; passing turn.");
      session.advanceTurnWithPass();
      return true;
    }
    if(!session.getBoard().makeMove(move.row(), move.col(), computerColor)){
      LOGGER.error("Computer strategy returned an invalid move at ({}, {})", move.row(), move.col());
      throw new IllegalStateException("Computer strategy produced invalid move");
    }
    LOGGER.info("Computer {} moved at ({}, {})",computerColor, move.row(), move.col());
    session.advanceTurnWithPass();
    return true;
  }

  private boolean isComputerTurn(GameSession session) {
    Player current = session.getCurrentPlayer();
    return current != null && current.isComputer();
  }
}

