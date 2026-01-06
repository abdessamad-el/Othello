package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.Position;

public interface ComputerStrategy {
   Position execute(GameSession session, PlayerColor computerColor);
}
