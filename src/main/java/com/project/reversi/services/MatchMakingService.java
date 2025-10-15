package com.project.reversi.services;

import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.dto.MatchStatusDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MatchMakingTicket;
import com.project.reversi.model.MatchStatus;
import com.project.reversi.model.Player;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MatchMakingService {

  private final ConcurrentLinkedQueue<MatchMakingTicket> waitingTickets = new ConcurrentLinkedQueue<>();
  private final Map<UUID, MatchStatus> ticketStatusMap = new ConcurrentHashMap<>();
  private final Map<UUID, MatchMakingTicket> activeTickets = new ConcurrentHashMap<>();
  private final Map<UUID, GameSession> completedSessions = new ConcurrentHashMap<>();
  private final Map<UUID, Color> ticketColors = new ConcurrentHashMap<>();
  private final GameSessionService gameSessionService;
  private final SimpMessagingTemplate messagingTemplate;

  public MatchMakingService(GameSessionService gameSessionService, SimpMessagingTemplate messagingTemplate) {
    this.gameSessionService = gameSessionService;
    this.messagingTemplate = messagingTemplate;
  }

  public UUID enqueue(String nickName, String preferredColor) {
    Color colorPreference = parsePreferredColor(preferredColor);
    MatchMakingTicket ticket = new MatchMakingTicket(nickName, colorPreference);
    waitingTickets.add(ticket);
    activeTickets.put(ticket.ticketId(), ticket);
    ticketStatusMap.put(ticket.ticketId(), MatchStatus.WAITING);
    tryMatch();
    return ticket.ticketId();
  }

  public boolean cancel(UUID ticketId) {
    MatchMakingTicket ticket = activeTickets.remove(ticketId);
    if (ticket != null && waitingTickets.remove(ticket)) {
      ticketStatusMap.put(ticketId, MatchStatus.CANCELED);
      completedSessions.remove(ticketId);
      ticketColors.remove(ticketId);
      notifyTicket(ticketId, MatchStatus.CANCELED, null, null);
      return true;
    }
    return false;
  }

  public Optional<GameSession> tryMatch() {
    MatchMakingTicket first = waitingTickets.poll();
    MatchMakingTicket second = waitingTickets.poll();

    if (first == null || second == null) {
      if (first != null) {
        waitingTickets.add(first);
      }
      return Optional.empty();
    }

    ColorAssignment assignment = assignColors(first, second);

    Player player1 = new Player(assignment.colorForFirst(), first.nickName());
    Player player2 = new Player(assignment.colorForSecond(), second.nickName());

    GameSession session = gameSessionService.createGameSession(GameType.PLAYER_VS_PLAYER, player1);
    gameSessionService.joinGameSession(session.getSessionId(), player2);

    ticketStatusMap.put(first.ticketId(), MatchStatus.FOUND);
    ticketStatusMap.put(second.ticketId(), MatchStatus.FOUND);
    completedSessions.put(first.ticketId(), session);
    completedSessions.put(second.ticketId(), session);
    ticketColors.put(first.ticketId(), assignment.colorForFirst());
    ticketColors.put(second.ticketId(), assignment.colorForSecond());
    activeTickets.remove(first.ticketId());
    activeTickets.remove(second.ticketId());

    notifyTicket(first.ticketId(), MatchStatus.FOUND, session, assignment.colorForFirst());
    notifyTicket(second.ticketId(), MatchStatus.FOUND, session, assignment.colorForSecond());

    return Optional.of(session);
  }

  public Optional<GameSession> getSessionByTicketId(UUID ticketId) {
    return Optional.ofNullable(completedSessions.get(ticketId));
  }

  public MatchStatus getStatus(UUID ticketId) {
    return ticketStatusMap.get(ticketId);
  }

  public Optional<Color> getAssignedColor(UUID ticketId) {
    return Optional.ofNullable(ticketColors.get(ticketId));
  }

  private ColorAssignment assignColors(MatchMakingTicket first, MatchMakingTicket second) {
    Color desiredFirst = first.preferredColor();
    Color desiredSecond = second.preferredColor();

    if (desiredFirst == null && desiredSecond == null) {
      return new ColorAssignment(Color.BLACK, Color.WHITE);
    }

    if (desiredFirst != null && desiredSecond == null) {
      return new ColorAssignment(desiredFirst, oppositeColor(desiredFirst));
    }

    if (desiredFirst == null && desiredSecond != null) {
      return new ColorAssignment(oppositeColor(desiredSecond), desiredSecond);
    }

    if (desiredFirst != null && desiredSecond != null && !desiredFirst.equals(desiredSecond)) {
      return new ColorAssignment(desiredFirst, desiredSecond);
    }

    Color assignedFirst = desiredFirst != null ? desiredFirst : Color.BLACK;
    return new ColorAssignment(assignedFirst, oppositeColor(assignedFirst));
  }

  private Color parsePreferredColor(String preferredColor) {
    if (preferredColor == null) {
      return null;
    }
    String normalized = preferredColor.trim().toUpperCase();
    return switch (normalized) {
      case "WHITE" -> Color.WHITE;
      case "BLACK" -> Color.BLACK;
      default -> null;
    };
  }

  private Color oppositeColor(Color color) {
    return Color.WHITE.equals(color) ? Color.BLACK : Color.WHITE;
  }

  private record ColorAssignment(Color colorForFirst, Color colorForSecond) {}

  private void notifyTicket(UUID ticketId, MatchStatus status, GameSession session, Color assignedColor) {
    if (messagingTemplate == null) {
      return;
    }
    GameSessionSummaryDTO summary = session != null ? GameSessionSummaryDTO.fromGameSession(session) : null;
    String assignedColorString = assignedColor != null ? (Color.WHITE.equals(assignedColor) ? "WHITE" : "BLACK") : null;
    MatchStatusDTO payload = new MatchStatusDTO(status.name(), summary, assignedColorString);
    messagingTemplate.convertAndSend("/topic/matchmaking/" + ticketId, payload);
  }
}
