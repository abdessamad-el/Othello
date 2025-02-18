package com.project.reversi.model;

import java.awt.Color;

public class Player {
  private Color color;
  private boolean computer;

  public Player(Color color) {
    this.color = color;
    this.computer = false;
  }

  public Player(Color color, boolean computer) {
    this.color = color;
    this.computer = computer;
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
}