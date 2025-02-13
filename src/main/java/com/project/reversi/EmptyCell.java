package com.project.reversi;

public class EmptyCell extends Cell {

  public EmptyCell(int row, int column) {
    super(row, column);
  }

  @Override
  public void printCell() {
    System.out.print(" ");
  }
}
