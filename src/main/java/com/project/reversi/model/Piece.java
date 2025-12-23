package com.project.reversi.model;

import com.project.reversi.model.Cell;

public class Piece extends Cell {
  private PlayerColor color;


  public Piece(int row, int column, PlayerColor color) {
    super(row, column);
    this.color = color;
  }


  @Override
  public void printCell() {
    System.out.print(color == PlayerColor.WHITE ? "W" : "B");
  }


  public String toString() {
    return color == PlayerColor.WHITE ? "W" : "B";
  }

  public void flip() {
    color = color == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
  }

  public PlayerColor getColor() {
    return color;
  }
}
