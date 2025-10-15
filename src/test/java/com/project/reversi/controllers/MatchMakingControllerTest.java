package com.project.reversi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.reversi.dto.EnqueueRequestDTO;
import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MatchStatus;
import com.project.reversi.model.Player;
import com.project.reversi.services.MatchMakingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.Color;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchMakingController.class)
class MatchMakingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private MatchMakingService matchMakingService;

  @Test
  @DisplayName("GET /api/matchmaking/{ticketId} returns waiting status")
  void getMatchStatusWaiting() throws Exception {
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.getStatus(ticketId)).thenReturn(MatchStatus.WAITING);
    Mockito.when(matchMakingService.getSessionByTicketId(ticketId)).thenReturn(Optional.empty());
    Mockito.when(matchMakingService.getAssignedColor(ticketId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/matchmaking/" + ticketId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status", is("WAITING")))
           .andExpect(jsonPath("$.assignedColor", nullValue()));
  }

  @Test
  @DisplayName("GET /api/matchmaking/{ticketId} returns session summary when found")
  void getMatchStatusFound() throws Exception {
    UUID ticketId = UUID.randomUUID();
    GameSession session = new GameSession(new Board(8, 8), new Player(Color.WHITE, "Alice"), GameType.PLAYER_VS_PLAYER);
    session.joinSession(new Player(Color.BLACK, "Bob"));

    Mockito.when(matchMakingService.getStatus(ticketId)).thenReturn(MatchStatus.FOUND);
    Mockito.when(matchMakingService.getSessionByTicketId(ticketId)).thenReturn(Optional.of(session));
    Mockito.when(matchMakingService.getAssignedColor(ticketId)).thenReturn(Optional.of(Color.WHITE));

    mockMvc.perform(get("/api/matchmaking/" + ticketId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status", is("FOUND")))
           .andExpect(jsonPath("$.gameSession.sessionId", is(session.getSessionId())))
           .andExpect(jsonPath("$.assignedColor", is("WHITE")))
           .andExpect(jsonPath("$.gameSession.playerNicknames[0]", is("Alice")))
           .andExpect(jsonPath("$.gameSession.playerNicknames[1]", is("Bob")));
  }

  @Test
  @DisplayName("GET /api/matchmaking/{ticketId} returns 404 when ticket missing")
  void getMatchStatusNotFound() throws Exception {
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.getStatus(ticketId)).thenReturn(null);

    mockMvc.perform(get("/api/matchmaking/" + ticketId))
           .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/matchmaking/enqueue returns ticket id")
  void enqueueReturnsTicketId() throws Exception {
    UUID ticketId = UUID.randomUUID();
    EnqueueRequestDTO request = new EnqueueRequestDTO("Alice", "WHITE");

    Mockito.when(matchMakingService.enqueue(request.nickName(), request.preferredColor())).thenReturn(ticketId);

    mockMvc.perform(post("/api/matchmaking/enqueue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$", is(ticketId.toString())));
  }

  @Test
  @DisplayName("DELETE /api/matchmaking/cancel/{ticketId} cancels ticket")
  void cancelTicket() throws Exception {
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.cancel(ticketId)).thenReturn(true);

    mockMvc.perform(delete("/api/matchmaking/cancel/" + ticketId))
           .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("DELETE /api/matchmaking/cancel/{ticketId} returns 404 when missing")
  void cancelTicketNotFound() throws Exception {
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.cancel(ticketId)).thenReturn(false);

    mockMvc.perform(delete("/api/matchmaking/cancel/" + ticketId))
           .andExpect(status().isNotFound());
  }
}
