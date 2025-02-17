package com.project.reversi.model;

public abstract class Cell {

  private final int row;
  private final int column;

  public Cell(int r, int c) {
    row = r;
    column = c;
  }


  public abstract void printCell();

  public int getRow() {
    return row;
  }

  public int getColumn() {
    return column;
  }

}
