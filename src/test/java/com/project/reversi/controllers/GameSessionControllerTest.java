package com.project.reversi.controllers;

import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.Player;
import com.project.reversi.services.GameSessionService;
import java.awt.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.project.reversi.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GameSessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameSessionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GameSessionService gameSessionService;

  @BeforeEach
  void setUpSecurityContext() {
    User user = new User("test-user", "password");
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }


  @Test
  @DisplayName("POST /api/session/create creates a session and returns summary")
  void createSession() throws Exception {
    GameSession session = new GameSession(new Board(8, 8), new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameSessionService.createGameSession(Mockito.eq(GameType.PLAYER_VS_PLAYER), Mockito.any()))
        .thenReturn(session);

    mockMvc.perform(post("/api/session/create")
            .param("gameType", "PLAYER_VS_PLAYER")
            .param("color", "WHITE")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId", notNullValue()))
        .andExpect(jsonPath("$.gameType", is("PLAYER_VS_PLAYER")))
        .andExpect(jsonPath("$.currentPlayerColor", is("WHITE")))
        .andExpect(jsonPath("$.currentPlayerNickname", notNullValue()))
        .andExpect(jsonPath("$.playerNicknames", notNullValue()))
        .andExpect(jsonPath("$.playerNicknames[0]", notNullValue()))
        .andExpect(jsonPath("$.board.boardCells", notNullValue()));
  }

  @Test
  @DisplayName("POST /api/session/{id}/join joins existing session")
  void joinSession() throws Exception {
    GameSession existing = new GameSession(new Board(8, 8), new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameSessionService.getSessionById(existing.getSessionId())).thenReturn(existing);

    GameSession afterJoin = existing;
    afterJoin.joinSession(new Player(Color.BLACK));
    Mockito.when(gameSessionService.joinGameSession(Mockito.eq(existing.getSessionId()), Mockito.any(Player.class)))
        .thenReturn(afterJoin);

    mockMvc.perform(post("/api/session/" + existing.getSessionId() + "/join")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sessionId", is(existing.getSessionId())))
        .andExpect(jsonPath("$.playerColors[0]", anyOf(is("WHITE"), is("BLACK"))))
        .andExpect(jsonPath("$.playerColors[1]", anyOf(is("WHITE"), is("BLACK"))))
        .andExpect(jsonPath("$.playerNicknames", notNullValue()));
  }

  @Test
  @DisplayName("GET /api/session/{id} returns 404 when not found")
  void getSessionNotFound() throws Exception {
    Mockito.when(gameSessionService.getSessionById("missing")).thenReturn(null);

    mockMvc.perform(get("/api/session/missing"))
        .andExpect(status().isNotFound());
  }
}



