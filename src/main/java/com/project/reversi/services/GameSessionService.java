package com.project.reversi.services;

import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.Player;
import com.project.reversi.repository.JpaGameSessionRepository;
import org.springframework.stereotype.Service;

@Service
public class GameSessionService {

  private final JpaGameSessionRepository sessionRepository;

  public GameSessionService(JpaGameSessionRepository sessionRepository) {
    this.sessionRepository = sessionRepository;
  }

  /**
   * Creates a new game session with a creator player.
   *
   * @param gameType the type of game (PLAYER_VS_PLAYER or PLAYER_VS_COMPUTER)
   * @param creator the player creating the session
   * @return the created GameSession
   */
  public GameSession createGameSession(GameType gameType, Player creator) {
    Board board = new Board(8, 8);
    GameSession session = new GameSession(board, creator, gameType);
    return sessionRepository.save(session);
  }

  /**
   * Retrieves a game session by its session ID.
   *
   * @param sessionId the unique identifier for the game session
   * @return the GameSession, or null if not found
   */
  public GameSession getSessionById(String sessionId) {
    return sessionRepository.findById(sessionId).orElse(null);
  }

  /**
   * Allows a second player to join a game session (only valid for PLAYER_VS_PLAYER mode).
   *
   * @param sessionId the session ID
   * @param joiningPlayer the player joining the session
   * @return the updated GameSession
   */
  public GameSession joinGameSession(String sessionId, Player joiningPlayer) {
    GameSession session = sessionRepository.findById(sessionId).orElse(null);
    if (session == null) {
      throw new IllegalArgumentException("Session not found");
    }
    session.joinSession(joiningPlayer);
    return sessionRepository.save(session);
  }

}
