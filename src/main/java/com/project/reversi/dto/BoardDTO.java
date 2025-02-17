package com.project.reversi.dto;

import java.util.ArrayList;
import java.util.List;

import com.project.reversi.model.Board;

public class BoardDTO {
  private List<List<String>> boardCells;

  public List<List<String>> getBoardCells() {
    return boardCells;
  }

  public void setBoardCells(List<List<String>> boardCells) {
    this.boardCells = boardCells;
  }


  public static BoardDTO fromBoard(Board board) {
    BoardDTO dto = new BoardDTO();
    List<List<String>> cells = new ArrayList<>();
    for (int i = 0; i < board.getNumRows(); i++) {
      List<String> row = new ArrayList<>();
      for (int j = 0; j < board.getNumColumns(); j++) {
        row.add(board.getCell(i, j).toString());
      }
      cells.add(row);
    }
    dto.setBoardCells(cells);
    return dto;
  }
}

