package com.project.reversi.services;

import com.project.reversi.model.GameSession;

import java.awt.*;

public interface ComputerStrategy {
   int[] execute(GameSession session, Color computerColor);
}
