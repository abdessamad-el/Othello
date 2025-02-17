
package com.project.reversi.model;

import com.project.reversi.model.Othello;

import java.awt.*;

public class Player {
  private final Othello game;
  private final Color color;

  public Player(Color c, Othello game) {
    color = c;
    this.game = game;
  }

  public boolean playPiece(int row, int column) {
    return game.getBoard().makeMove(row, column, color, false);

  }

}
