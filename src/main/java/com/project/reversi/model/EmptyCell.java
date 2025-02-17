package com.project.reversi.model;

import com.project.reversi.model.Cell;

public class EmptyCell extends Cell {

  public EmptyCell(int row, int column) {
    super(row, column);
  }

  @Override
  public void printCell() {
    System.out.print("-");
  }
  @Override
  public String toString(){
    return "-";
  }
}
