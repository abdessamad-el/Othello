package com.project.reversi.dto;

import com.project.reversi.model.PlayerColor;

public class MoveRequestDTO {
  private int row;
  private int column;
  private PlayerColor color;
  private String sessionId;


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

  public PlayerColor getColor() {
    return color;
  }

  public void setColor(PlayerColor color) {
    this.color = color;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
}

