package com.project.reversi.model;


public class Piece {
  private PlayerColor color;


  public Piece(PlayerColor color) {
    this.color = color;
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
