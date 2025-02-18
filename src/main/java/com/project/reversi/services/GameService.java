package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.Player;
import com.project.reversi.repository.GameSessionRepository;
import org.springframework.stereotype.Service;

import java.awt.Color;

@Service
public class GameService {

  private final GameSessionRepository sessionRepository;

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
   * @return true if the move is successful, false otherwise.
   */
  public boolean makeMove(String sessionId, int row, int column, Color playerColor) {
    GameSession session = sessionRepository.findById(sessionId);
    if (session == null) {
      throw new IllegalArgumentException("Session not found");
    }
    // Ensure it's the correct player's turn.
    Player currentPlayer = session.getCurrentPlayer();
    if (!currentPlayer.getColor().equals(playerColor)) {
      throw new IllegalArgumentException("It's not the turn of the player with this color");
    }
    // Attempt the move on the board
    boolean moveResult = session.getBoard().makeMove(row, column, playerColor, false);
    if (moveResult) {
      session.advanceTurn();
      sessionRepository.save(session);
    }
    return moveResult;
  }

  public GameSession getSessionById(String sessionId) {
    return sessionRepository.findById(sessionId);
  }

}
