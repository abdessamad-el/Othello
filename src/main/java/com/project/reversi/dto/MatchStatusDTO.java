package com.project.reversi.dto;

public class MatchStatusDTO {
    private final String status;
    private final GameSessionSummaryDTO gameSession;
    private final String assignedColor;
    private final String playerToken;

    public MatchStatusDTO(String status, GameSessionSummaryDTO gameSession, String assignedColor, String playerToken) {
        this.status = status;
        this.gameSession = gameSession;
        this.assignedColor = assignedColor;
        this.playerToken = playerToken;
    }

    public String getStatus() {
        return status;
    }

    public GameSessionSummaryDTO getGameSession() {
        return gameSession;
    }

    public String getAssignedColor() {
        return assignedColor;
    }

    public String getPlayerToken() {
        return playerToken;
    }
}
