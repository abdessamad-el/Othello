package com.project.reversi.controllers;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.reversi.dto.MoveRequestDTO;
import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameType;
import com.project.reversi.model.MoveResult;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.Position;
import com.project.reversi.model.User;
import com.project.reversi.services.GameService;
import com.project.reversi.services.GameSessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SessionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GameSessionService gameSessionService;

  @MockBean
  private GameService gameService;

  @MockBean
  private SimpMessagingTemplate simpMessagingTemplate;

  @Autowired
  private ObjectMapper objectMapper;

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
  @DisplayName("POST /api/v1/sessions/ creates a session and returns summary")
  void createSession() throws Exception {
    GameSession session = new GameSession(new Board(8, 8), new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameSessionService.createGameSession(Mockito.eq(GameType.PLAYER_VS_PLAYER), Mockito.any()))
           .thenReturn(session);

    mockMvc.perform(post("/api/v1/sessions")
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
  @DisplayName("POST /api/v1/sessions/{id}/join joins existing session")
  void joinSession() throws Exception {
    GameSession existing = new GameSession(new Board(8, 8), new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameSessionService.getSessionById(existing.getSessionId())).thenReturn(existing);

    GameSession afterJoin = existing;
    afterJoin.joinSession(new Player(PlayerColor.BLACK));
    Mockito.when(gameSessionService.joinGameSession(Mockito.eq(existing.getSessionId()), Mockito.any(Player.class)))
           .thenReturn(afterJoin);

    mockMvc.perform(post("/api/v1/sessions/" + existing.getSessionId() + "/join")
                        .contentType(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.sessionId", is(existing.getSessionId())))
           .andExpect(jsonPath("$.playerColors[0]", anyOf(is("WHITE"), is("BLACK"))))
           .andExpect(jsonPath("$.playerColors[1]", anyOf(is("WHITE"), is("BLACK"))))
           .andExpect(jsonPath("$.playerNicknames", notNullValue()));
  }

  @Test
  @DisplayName("GET /api/v1/sessions/{id} returns 404 when not found")
  void getSessionNotFound() throws Exception {
    Mockito.when(gameSessionService.getSessionById("missing")).thenReturn(null);

    mockMvc.perform(get("/api/v1/sessions/missing"))
           .andExpect(status().isNotFound());
  }


  @Test
  @DisplayName("GET /api/v1/sessions/{id}/board returns board DTO for session")
  void getBoard() throws Exception {
    GameSession session = new GameSession(new Board(8, 8), new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameService.getSessionById(session.getSessionId())).thenReturn(session);

    mockMvc.perform(get("/api/v1/sessions/" + session.getSessionId() + "/board"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.boardCells", notNullValue()))
           .andExpect(jsonPath("$.boardCells[3][3]", anyOf(is("W"), is("-"))));
  }

  @Test
  @DisplayName("GET /api/v1/sessions/{id}/possible-moves?color={color} returns computed moves")
  void getPossibleMoves() throws Exception {
    Board board = new Board(8, 8);
    GameSession session = new GameSession(board, new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER) {
      @Override
      public java.util.List<Position> computeValidMoves(PlayerColor color) {
        return Arrays.asList(new Position(2, 3), new Position(4, 5));
      }
    };
    Mockito.when(gameService.getSessionById(session.getSessionId())).thenReturn(session);
    mockMvc.perform(get("/api/v1/sessions/" + session.getSessionId() + "/possible-moves")
                        .param("color", "WHITE")
                        .contentType(MediaType.APPLICATION_JSON)
                        )
           .andExpect(status().isOk())
           .andExpect(jsonPath("$", hasSize(2)))
           .andExpect(jsonPath("$[0].row", is(2)))
           .andExpect(jsonPath("$[0].column", is(3)))
           .andExpect(jsonPath("$[1].row", is(4)))
           .andExpect(jsonPath("$[1].column", is(5)));
  }

  @Test
  @DisplayName("POST /api/v1/sessions/{id}/moves maps MoveResult to message and returns summary")
  void makeMoveSuccess() throws Exception {
    GameSession session = new GameSession(new Board(8, 8), new Player(PlayerColor.WHITE), GameType.PLAYER_VS_PLAYER);
    Mockito.when(gameService.makeMove(Mockito.eq(session.getSessionId()), Mockito.anyInt(), Mockito.anyInt(), Mockito.eq(PlayerColor.WHITE)))
           .thenReturn(MoveResult.SUCCESS);
    Mockito.when(gameService.getSessionById(session.getSessionId())).thenReturn(session);

    MoveRequestDTO req = new MoveRequestDTO();
    req.setRow(2);
    req.setColumn(3);
    req.setColor(PlayerColor.WHITE);

    mockMvc.perform(post("/api/v1/sessions/" + session.getSessionId() + "/moves")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.message", is("Move successful")))
           .andExpect(jsonPath("$.sessionSummary.sessionId", is(session.getSessionId())))
           .andExpect(jsonPath("$.sessionSummary.playerNicknames", notNullValue()));
  }
}