package com.project.reversi.services;

import com.project.reversi.model.GameSession;
import com.project.reversi.model.PlayerColor;

public interface ComputerStrategy {
   int[] execute(GameSession session, PlayerColor computerColor);
}
