package com.project.reversi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.reversi.dto.MoveRequestDTO;
import com.project.reversi.model.*;
import com.project.reversi.services.GameService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.*;
import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GameController.class)
class GameControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GameService gameService;

  @MockBean
  private SimpMessagingTemplate simpMessagingTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("GET /api/game/board returns board DTO for session")
  void getBoard() throws Exception {
    GameSession session = new GameSession(new Board(8, 8), new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameService.getSessionById(session.getSessionId())).thenReturn(session);

    mockMvc.perform(get("/api/game/board").param("sessionId", session.getSessionId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.boardCells", notNullValue()))
        .andExpect(jsonPath("$.boardCells[3][3]", anyOf(is("W"), is("-"))));
  }

  @Test
  @DisplayName("POST /api/game/possible-moves returns computed moves")
  void getPossibleMoves() throws Exception {
    // Build session with a board whose computeValidMoves returns a known set
    Board board = new Board(8, 8) {
      @Override
      public java.util.List<int[]> computeValidMoves(Color color) {
        return Arrays.asList(new int[]{2, 3}, new int[]{4, 5});
      }
    };
    GameSession session = new GameSession(board, new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameService.getSessionById(session.getSessionId())).thenReturn(session);

    MoveRequestDTO req = new MoveRequestDTO();
    req.setSessionId(session.getSessionId());
    req.setColor("WHITE");

    mockMvc.perform(post("/api/game/possible-moves")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].row", is(2)))
        .andExpect(jsonPath("$[0].column", is(3)))
        .andExpect(jsonPath("$[1].row", is(4)))
        .andExpect(jsonPath("$[1].column", is(5)));
  }

  @Test
  @DisplayName("POST /api/game/move maps MoveResult to message and returns summary")
  void makeMoveSuccess() throws Exception {
    GameSession session = new GameSession(new Board(8, 8), new Player(Color.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameService.makeMove(Mockito.eq(session.getSessionId()), Mockito.anyInt(), Mockito.anyInt(), Mockito.eq(Color.WHITE), Mockito.anyBoolean()))
        .thenReturn(MoveResult.SUCCESS);
    Mockito.when(gameService.getSessionById(session.getSessionId())).thenReturn(session);

    MoveRequestDTO req = new MoveRequestDTO();
    req.setSessionId(session.getSessionId());
    req.setRow(2);
    req.setColumn(3);
    req.setColor("WHITE");
    req.setPass(false);

    mockMvc.perform(post("/api/game/move")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message", is("Move successful")))
        .andExpect(jsonPath("$.sessionSummary.sessionId", is(session.getSessionId())));
  }
}

