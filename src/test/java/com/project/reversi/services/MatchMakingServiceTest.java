package com.project.reversi.services;

import com.project.reversi.dto.MatchStatusDTO;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MatchStatus;
import com.project.reversi.repository.JpaGameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.awt.Color;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DataJpaTest
class MatchMakingServiceTest {

  @Autowired
  private JpaGameSessionRepository repository;

  private MatchMakingService matchMakingService;
  private SimpMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    GameSessionService gameSessionService = new GameSessionService(repository);
    messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
    matchMakingService = new MatchMakingService(gameSessionService, messagingTemplate);
  }

  @Test
  void matchingAssignsOppositeColorsWhenBothPreferSame() {
    UUID firstTicket = matchMakingService.enqueue("Alice", "WHITE");
    UUID secondTicket = matchMakingService.enqueue("Bob", "WHITE");

    assertEquals(MatchStatus.FOUND, matchMakingService.getStatus(firstTicket));
    assertEquals(MatchStatus.FOUND, matchMakingService.getStatus(secondTicket));

    Optional<GameSession> sessionOptional = matchMakingService.getSessionByTicketId(firstTicket);
    assertTrue(sessionOptional.isPresent());

    GameSession session = sessionOptional.get();
    assertEquals(GameType.PLAYER_VS_PLAYER, session.getGameType());
    assertEquals(Color.WHITE, session.getPlayers().get(0).getColor());
    assertEquals(Color.BLACK, session.getPlayers().get(1).getColor());

    assertEquals(Color.WHITE, matchMakingService.getAssignedColor(firstTicket).orElse(null));
    assertEquals(Color.BLACK, matchMakingService.getAssignedColor(secondTicket).orElse(null));

    ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<MatchStatusDTO> payloadCaptor = ArgumentCaptor.forClass(MatchStatusDTO.class);
    verify(messagingTemplate, times(2)).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());

    assertTrue(destinationCaptor.getAllValues().stream().allMatch(dest -> dest.startsWith("/topic/matchmaking/")));
    assertTrue(payloadCaptor.getAllValues().stream().allMatch(dto -> "FOUND".equals(dto.getStatus())));
    assertTrue(payloadCaptor.getAllValues().stream().anyMatch(dto -> "WHITE".equals(dto.getAssignedColor())));
    assertTrue(payloadCaptor.getAllValues().stream().anyMatch(dto -> "BLACK".equals(dto.getAssignedColor())));
  }

  @Test
  void singleTicketRemainsWaiting() {
    UUID ticket = matchMakingService.enqueue("Solo", null);

    assertEquals(MatchStatus.WAITING, matchMakingService.getStatus(ticket));
    assertTrue(matchMakingService.getSessionByTicketId(ticket).isEmpty());
    verifyNoMoreInteractions(messagingTemplate);
  }

  @Test
  void cancelRemovesTicketFromQueue() {
    UUID ticket = matchMakingService.enqueue("CancelMe", "BLACK");
    assertTrue(matchMakingService.cancel(ticket));

    assertEquals(MatchStatus.CANCELED, matchMakingService.getStatus(ticket));
    assertTrue(matchMakingService.getSessionByTicketId(ticket).isEmpty());
    ArgumentCaptor<MatchStatusDTO> cancelPayload = ArgumentCaptor.forClass(MatchStatusDTO.class);
    verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/matchmaking/" + ticket), cancelPayload.capture());
    assertEquals("CANCELED", cancelPayload.getValue().getStatus());
    assertNull(cancelPayload.getValue().getAssignedColor());
  }

  @Test
  void mixedPreferencesAssignCorrectly() {
    UUID firstTicket = matchMakingService.enqueue("Alice", "WHITE");
    UUID secondTicket = matchMakingService.enqueue("Bob", "BLACK");

    GameSession session = matchMakingService.getSessionByTicketId(firstTicket).orElseThrow();
    assertEquals(Color.WHITE, session.getPlayers().get(0).getColor());
    assertEquals(Color.BLACK, session.getPlayers().get(1).getColor());
    assertEquals(Color.WHITE, matchMakingService.getAssignedColor(firstTicket).orElse(null));
    assertEquals(Color.BLACK, matchMakingService.getAssignedColor(secondTicket).orElse(null));
  }
}
