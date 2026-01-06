package com.project.reversi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.reversi.dto.EnqueueRequestDTO;
import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MatchStatus;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.User;
import com.project.reversi.services.MatchMakingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MatchMakingController.class)
@AutoConfigureMockMvc(addFilters = false)
class MatchMakingControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private MatchMakingService matchMakingService;

  @AfterEach
  void clearSecurity() {
    SecurityContextHolder.clearContext();
  }

  private User authenticateTestUser() {
    User user = new User("test-user", "password");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
    return user;
  }

  @Test
  @DisplayName("GET /api/v1/matches/{ticketId} returns waiting status")
  void getMatchStatusWaiting() throws Exception {
    authenticateTestUser();
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.getStatus(ticketId)).thenReturn(MatchStatus.WAITING);
    Mockito.when(matchMakingService.getSessionByTicketId(ticketId)).thenReturn(Optional.empty());
    Mockito.when(matchMakingService.getAssignedColor(ticketId)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/v1/matches/" + ticketId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status", is("WAITING")))
           .andExpect(jsonPath("$.assignedColor", nullValue()));
  }

  @Test
  @DisplayName("GET /api/v1/matches/auth-check returns OK for authenticated user")
  void authCheckReturnsOk() throws Exception {
    authenticateTestUser();

    mockMvc.perform(get("/api/v1/matches/auth-check"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /api/v1/matches/{ticketId} returns session summary when found")
  void getMatchStatusFound() throws Exception {
    UUID ticketId = UUID.randomUUID();
    GameSession session = new GameSession(new Board(8, 8), new Player(PlayerColor.WHITE, "Alice"), GameType.PLAYER_VS_PLAYER);
    session.joinSession(new Player(PlayerColor.BLACK, "Bob"));

    Mockito.when(matchMakingService.getStatus(ticketId)).thenReturn(MatchStatus.FOUND);
    Mockito.when(matchMakingService.getSessionByTicketId(ticketId)).thenReturn(Optional.of(session));
    Mockito.when(matchMakingService.getAssignedColor(ticketId)).thenReturn(Optional.of(PlayerColor.WHITE));

    mockMvc.perform(get("/api/v1/matches/" + ticketId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status", is("FOUND")))
           .andExpect(jsonPath("$.gameSession.sessionId", is(session.getSessionId())))
           .andExpect(jsonPath("$.assignedColor", is("WHITE")))
           .andExpect(jsonPath("$.gameSession.playerNicknames[0]", is("Alice")))
           .andExpect(jsonPath("$.gameSession.playerNicknames[1]", is("Bob")));
  }

  @Test
  @DisplayName("GET /api/matches/{ticketId} returns 404 when ticket missing")
  void getMatchStatusNotFound() throws Exception {
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.getStatus(ticketId)).thenReturn(null);

    mockMvc.perform(get("/api/matches/" + ticketId))
           .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/v1/matches returns ticket id")
  void enqueueReturnsTicketId() throws Exception {
    User user = authenticateTestUser();
    UUID ticketId = UUID.randomUUID();
    EnqueueRequestDTO request = new EnqueueRequestDTO("WHITE");

    Mockito.when(matchMakingService.enqueue(user, request.preferredColor())).thenReturn(ticketId);

    mockMvc.perform(post("/api/v1/matches/")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsBytes(request)))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$", is(ticketId.toString())));
  }

  @Test
  @DisplayName("DELETE /api/v1/matches/{ticketId} cancels ticket")
  void cancelTicket() throws Exception {
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.cancel(ticketId)).thenReturn(true);

    mockMvc.perform(delete("/api/v1/matches/" + ticketId))
           .andExpect(status().isNoContent());
  }

  @Test
  @DisplayName("DELETE /api/matches/v1/{ticketId} returns 404 when missing")
  void cancelTicketNotFound() throws Exception {
    UUID ticketId = UUID.randomUUID();
    Mockito.when(matchMakingService.cancel(ticketId)).thenReturn(false);

    mockMvc.perform(delete("/api/matches/v1/" + ticketId))
           .andExpect(status().isNotFound());
  }
}
