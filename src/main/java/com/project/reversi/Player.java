

package com.project.reversi;

import java.awt.*;

public class Player {
  private Othello _game;
  private Color _color;

  public Player(Color c, Othello game) {
    _color = c;
    _game = game;
  }


  public Player() {

  }

  public boolean playPiece(int row, int column) {
    return _game.getBoard().makeMove(row, column, _color, false);

  }

}
