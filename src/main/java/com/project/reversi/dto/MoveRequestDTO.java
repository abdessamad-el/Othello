package com.project.reversi.dto;

public class MoveRequestDTO {
  private int row;
  private int column;
  private String color; // Expected values: "WHITE" or "BLACK"
  private String sessionId;
  private Boolean pass; // field for passing the turn


  public Boolean getPass() {
    return pass;
  }

  public void setPass(Boolean pass) {
    this.pass = pass;
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

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }
}

