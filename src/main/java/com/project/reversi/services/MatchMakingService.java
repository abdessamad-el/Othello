package com.project.reversi.services;

import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.dto.MatchStatusDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MatchMakingTicket;
import com.project.reversi.model.MatchStatus;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.User;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MatchMakingService {

  private final ConcurrentLinkedQueue<UUID> waitingTickets = new ConcurrentLinkedQueue<>();
  private final Map<UUID, TicketState> tickets = new ConcurrentHashMap<>();
  private final java.util.Set<String> waitingUsers = ConcurrentHashMap.newKeySet();
  private final GameSessionService gameSessionService;
  private final SimpMessagingTemplate messagingTemplate;

  public MatchMakingService(GameSessionService gameSessionService, SimpMessagingTemplate messagingTemplate) {
    this.gameSessionService = gameSessionService;
    this.messagingTemplate = messagingTemplate;
  }

  public UUID enqueue(User user, String preferredColor) {
    if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
      throw new IllegalArgumentException("Authenticated user required for matchmaking");
    }
    String username = user.getUsername();
    boolean added = waitingUsers.add(username);
    if (!added) {
      throw new IllegalStateException("User already in matchmaking queue");
    }

    PlayerColor colorPreference = parsePreferredColor(preferredColor);
    MatchMakingTicket ticket = new MatchMakingTicket(username, colorPreference);
    TicketState state = new TicketState(ticket, user);
    tickets.put(ticket.ticketId(), state);
    waitingTickets.add(ticket.ticketId());
    tryMatch();
    return ticket.ticketId();
  }

  public boolean cancel(UUID ticketId) {
    TicketState state = tickets.get(ticketId);
    if (state == null || state.status != MatchStatus.WAITING) {
      return false;
    }
    waitingTickets.remove(ticketId);
    state.status = MatchStatus.CANCELED;
    state.session = null;
    state.assignedColor = null;
    String ownerName = state.ownerUsername();
    if (ownerName != null) {
      waitingUsers.remove(ownerName);
    }
    notifyTicket(ticketId, MatchStatus.CANCELED, null, null);
    return true;
  }

  public Optional<GameSession> tryMatch() {
    TicketState first = pollNextWaitingTicket();
    TicketState second = pollNextWaitingTicket();

    if (first == null || second == null) {
      if (first != null) {
        waitingTickets.add(first.ticket.ticketId());
      }
      if (second != null) {
        waitingTickets.add(second.ticket.ticketId());
      }
      return Optional.empty();
    }

    ColorAssignment assignment = assignColors(first.ticket, second.ticket);

    Player player1 = new Player(assignment.colorForFirst(), first.ticket.username());
    Player player2 = new Player(assignment.colorForSecond(), second.ticket.username());
    attachOwner(player1, first.owner);
    attachOwner(player2, second.owner);

    GameSession session = gameSessionService.createGameSession(GameType.PLAYER_VS_PLAYER, player1);
    gameSessionService.joinGameSession(session.getSessionId(), player2);

    first.status = MatchStatus.FOUND;
    second.status = MatchStatus.FOUND;
    first.assignedColor = assignment.colorForFirst();
    second.assignedColor = assignment.colorForSecond();
    first.session = session;
    second.session = session;

    String firstOwner = first.ownerUsername();
    String secondOwner = second.ownerUsername();
    if (firstOwner != null) {
      waitingUsers.remove(firstOwner);
    }
    if (secondOwner != null) {
      waitingUsers.remove(secondOwner);
    }

    notifyTicket(first.ticket.ticketId(), MatchStatus.FOUND, session, first.assignedColor);
    notifyTicket(second.ticket.ticketId(), MatchStatus.FOUND, session, second.assignedColor);

    return Optional.of(session);
  }

  public Optional<GameSession> getSessionByTicketId(UUID ticketId) {
    TicketState state = tickets.get(ticketId);
    return state != null ? Optional.ofNullable(state.session) : Optional.empty();
  }

  public MatchStatus getStatus(UUID ticketId) {
    TicketState state = tickets.get(ticketId);
    return state != null ? state.status : null;
  }

  public Optional<PlayerColor> getAssignedColor(UUID ticketId) {
    TicketState state = tickets.get(ticketId);
    return state != null ? Optional.ofNullable(state.assignedColor) : Optional.empty();
  }

  private TicketState pollNextWaitingTicket() {
    UUID id;
    while ((id = waitingTickets.poll()) != null) {
      TicketState state = tickets.get(id);
      if (state == null) {
        continue;
      }
      if (state.status == MatchStatus.WAITING) {
        return state;
      }
    }
    return null;
  }

  private void attachOwner(Player player, User owner) {
    if (owner == null) {
      return;
    }
    player.setAccount(owner);
    if (player.getNickName() == null || player.getNickName().isBlank()) {
      player.setNickName(owner.getUsername());
    }
  }

  private ColorAssignment assignColors(MatchMakingTicket first, MatchMakingTicket second) {
    PlayerColor desiredFirst = first.preferredColor();
    PlayerColor desiredSecond = second.preferredColor();

    if (desiredFirst == null && desiredSecond == null) {
      return new ColorAssignment(PlayerColor.BLACK, PlayerColor.WHITE);
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

    PlayerColor assignedFirst = desiredFirst != null ? desiredFirst : PlayerColor.BLACK;
    return new ColorAssignment(assignedFirst, oppositeColor(assignedFirst));
  }

  private PlayerColor parsePreferredColor(String preferredColor) {
    if (preferredColor == null) {
      return null;
    }
    String normalized = preferredColor.trim().toUpperCase();
    return switch (normalized) {
      case "WHITE" -> PlayerColor.WHITE;
      case "BLACK" -> PlayerColor.BLACK;
      default -> null;
    };
  }

  private PlayerColor oppositeColor(PlayerColor color) {
    return color == PlayerColor.WHITE ? PlayerColor.BLACK : PlayerColor.WHITE;
  }

  private record ColorAssignment(PlayerColor colorForFirst, PlayerColor colorForSecond) {}

  private void notifyTicket(UUID ticketId, MatchStatus status, GameSession session, PlayerColor assignedColor) {
    if (messagingTemplate == null) {
      return;
    }
    GameSessionSummaryDTO summary = session != null ? GameSessionSummaryDTO.fromGameSession(session) : null;
    String assignedColorString = assignedColor != null ? (assignedColor == PlayerColor.WHITE ? "WHITE" : "BLACK") : null;
    MatchStatusDTO payload = new MatchStatusDTO(status.name(), summary, assignedColorString);
    messagingTemplate.convertAndSend("/topic/matchmaking/" + ticketId, payload);
  }

  private static class TicketState {
    private final MatchMakingTicket ticket;
    private final User owner;
    private volatile MatchStatus status;
    private volatile PlayerColor assignedColor;
    private volatile GameSession session;

    private TicketState(MatchMakingTicket ticket, User owner) {
      this.ticket = ticket;
      this.owner = owner;
      this.status = MatchStatus.WAITING;
    }

    private String ownerUsername() {
      return owner != null ? owner.getUsername() : null;
    }
  }
}
