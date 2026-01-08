package com.project.reversi.dto;

public interface LeaderboardRow {

  Long getUserId();

  String getUsername();

  Long getGames();

  Long getWins();

  Long getLosses();

  Long getDraws();

  Double getWinRate();

  Long getRank();
}
