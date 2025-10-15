package com.project.reversi.dto;

public class MatchStatusDTO {
    private final String status;
    private final GameSessionSummaryDTO gameSession;
    private final String assignedColor;

    public MatchStatusDTO(String status, GameSessionSummaryDTO gameSession, String assignedColor) {
        this.status = status;
        this.gameSession = gameSession;
        this.assignedColor = assignedColor;
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
}
