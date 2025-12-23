package com.project.reversi.model;



import org.springframework.lang.Nullable;
import java.util.UUID;
import java.time.LocalDateTime;

public record MatchMakingTicket(UUID ticketId, String username, LocalDateTime createdAt, @Nullable PlayerColor preferredColor) {

  public MatchMakingTicket(String username, PlayerColor preferredColor) {
    this(UUID.randomUUID(), username, LocalDateTime.now(), preferredColor);
  }
}
