package com.project.reversi.model;



import org.springframework.lang.Nullable;
import java.util.UUID;
import java.time.LocalDateTime;
import java.awt.Color;

public record MatchMakingTicket(UUID ticketId, String username, LocalDateTime createdAt, @Nullable Color preferredColor) {

  public MatchMakingTicket(String username, Color preferredColor) {
    this(UUID.randomUUID(), username, LocalDateTime.now(), preferredColor);
  }
}
