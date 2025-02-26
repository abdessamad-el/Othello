package com.project.reversi.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;

public class Player {
  private Color color;
  private boolean computer;
  private GameSession game;


  private static final Logger logger = LoggerFactory.getLogger(Player.class);

  public Player(Color color) {
    this.color = color;
    this.computer = false;
  }

  public Player(Color color, boolean computer) {
    this.color = color;
    this.computer = computer;
  }

  public Player(Color color, boolean computer, GameSession session) {
    this(color, computer);
    this.game = session;
    if (isComputer()) {
      session.OnChanged().add(this::performComputerMove);
    }
  }

  public Color getColor() {
    return color;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public boolean isComputer() {
    return computer;
  }

  public void setComputer(boolean computer) {
    this.computer = computer;
  }

  @Override
  public String toString() {
    return computer ? "Computer(" + color.toString() + ")" : "Player(" + color.toString() + ")";
  }

  private void performComputerMove() {
    Color computerColor = game.getCurrentPlayer().getColor();

    // Check if the computer has any valid moves.
    if (!game.getBoard().hasValidMove(computerColor)) {
      logger.info("Computer has no valid moves; passing turn.");
      game.advanceTurn();
      return;
    }
    // Otherwise, attempt to find a valid move.
    boolean moveMade = false;
    for (int i = 0; i < game.getBoard().getNumRows() && !moveMade; i++) {
      for (int j = 0; j < game.getBoard().getNumColumns() && !moveMade; j++) {
        if (game.getBoard().makeMove(i, j, computerColor, false)) {
          logger.info("Computer moved at ({}, {})", i, j);
          moveMade = true;
        }
      }
    }
    // advance the turn and save the session.
    game.advanceTurn();
  }
}