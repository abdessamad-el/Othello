package com.project.reversi.services;

import java.awt.Color;

import com.project.reversi.model.Board;
import org.springframework.stereotype.Service;

@Service
public class GameService {
  private final Board board;

  public GameService() {
    // Initialize your board with the appropriate dimensions
    this.board = new Board(8, 8);
  }

  public Board getBoard() {
    return board;
  }

  public boolean makeMove(int row, int column, String colorStr) {
    Color color = "BLACK".equalsIgnoreCase(colorStr) ? Color.BLACK : Color.WHITE;
    return board.makeMove(row, column, color, false);
  }
}

