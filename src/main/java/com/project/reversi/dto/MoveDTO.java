package com.project.reversi.dto;

public class MoveDTO {

  private int row;
  private int column;

  public MoveDTO(int r, int c) {
    row = r;
    column = c;
  }

  public int getRow() {
    return row;
  }

  public void setRow(int row) {
    this.row = row;
  }

  public int getColumn() {
    return column;
  }

  public void setColumn(int column) {
    this.column = column;
  }

}
