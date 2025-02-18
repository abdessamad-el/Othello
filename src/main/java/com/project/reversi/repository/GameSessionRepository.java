package com.project.reversi.repository;


import com.project.reversi.model.GameSession;

public interface GameSessionRepository {
  /**
   * Save a game session. If the session already exists, update it.
   *
   * @param session the game session to save
   * @return the saved game session
   */
  GameSession save(GameSession session);

  /**
   * Find a game session by its unique session ID.
   *
   * @param sessionId the session's unique identifier
   * @return the corresponding GameSession, or null if not found
   */
  GameSession findById(String sessionId);

  /**
   * Delete a game session by its session ID.
   *
   * @param sessionId the session's unique identifier
   */
  void delete(String sessionId);
}

