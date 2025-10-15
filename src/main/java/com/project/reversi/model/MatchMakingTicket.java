package com.project.reversi.model;



import org.springframework.lang.Nullable;
import java.util.UUID;
import java.time.LocalDateTime;
import java.awt.Color;

public record MatchMakingTicket(UUID ticketId, String nickName, LocalDateTime createdAt, @Nullable Color preferredColor) {

    
    public MatchMakingTicket(String nickName , Color preferredColor) {
        this(UUID.randomUUID(), nickName, LocalDateTime.now(), preferredColor);
    }
}