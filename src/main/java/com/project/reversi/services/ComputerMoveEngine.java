package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.Piece;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComputerMoveEngine {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComputerMoveEngine.class);

  @Autowired
  private ComputerStrategy strategy;


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
    
    int[] move = strategy.execute(session,computerColor);
    if (move[0] == -1 && move[1] == -1){
      LOGGER.info("Computer has no valid moves; passing turn.");
      session.advanceTurn();
      return true;
    }
    List<Piece> result = session.getBoard().makeMove(move[0], move[1], computerColor, false);
    if(result.isEmpty()){
      LOGGER.error("Computer strategy returned an invalid move at ({}, {})", move[0], move[1]);
      return false;
    }
    LOGGER.info("Computer {} moved at ({}, {})",computerColor, move[0], move[1]);
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

