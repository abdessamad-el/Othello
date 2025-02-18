package com.project.reversi.repository;

import com.project.reversi.model.GameSession;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryGameSessionRepository implements GameSessionRepository {

  private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

  @Override
  public GameSession save(GameSession session) {
    sessions.put(session.getSessionId(), session);
    return session;
  }

  @Override
  public GameSession findById(String sessionId) {
    return sessions.get(sessionId);
  }

  @Override
  public void delete(String sessionId) {
    sessions.remove(sessionId);
  }
}
