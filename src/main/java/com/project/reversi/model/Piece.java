package com.project.reversi.model;

import com.project.reversi.model.Cell;

import java.awt.*;

public class Piece extends Cell {
  private Color color;


  public Piece(int row, int column, Color color) {
    super(row, column);
    this.color = color;
  }


  @Override
  public void printCell() {
    System.out.print(color.equals(Color.WHITE) ? "W" : "B");
  }


  public String toString() {
    return color.equals(Color.WHITE) ? "W" : "B";
  }

  public void flip() {
    color = color.equals(Color.WHITE) ? Color.BLACK : Color.WHITE;
  }

  public Color getColor() {
    return color;
  }
}
