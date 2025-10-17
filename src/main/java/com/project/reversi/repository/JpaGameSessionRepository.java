package com.project.reversi.repository;

import com.project.reversi.model.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaGameSessionRepository extends JpaRepository<GameSession, String> {
}
