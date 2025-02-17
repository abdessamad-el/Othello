package com.project.reversi.dto;

public class MoveRequestDTO {
  private int row;
  private int column;
  private String color; // Expected values: "WHITE" or "BLACK"

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

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }
}

