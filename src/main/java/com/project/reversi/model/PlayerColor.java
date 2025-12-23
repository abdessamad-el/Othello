package com.project.reversi.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum PlayerColor {
  WHITE,
  BLACK;

  @JsonCreator
  public static PlayerColor fromString(String value) {
    if (value == null) {
      return null;
    }
    return PlayerColor.valueOf(value.trim().toUpperCase());
  }

  public PlayerColor opposite() {
    return this == WHITE ? BLACK : WHITE;
  }
}
