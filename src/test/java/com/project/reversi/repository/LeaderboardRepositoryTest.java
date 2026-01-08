package com.project.reversi.repository;

import com.project.reversi.dto.LeaderboardRow;
import com.project.reversi.model.Board;
import com.project.reversi.model.GameSession;
import com.project.reversi.model.GameState;
import com.project.reversi.model.GameType;
import com.project.reversi.model.Player;
import com.project.reversi.model.PlayerColor;
import com.project.reversi.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class LeaderboardRepositoryTest {

  @Autowired
  private LeaderboardRepository leaderboardRepository;

  @Autowired
  private JpaGameSessionRepository sessionRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void aggregatesLeaderboardRowsFromFinishedSessions() {
    User alice = userRepository.save(new User("alice", "pw"));
    User bob = userRepository.save(new User("bob", "pw"));
    User carol = userRepository.save(new User("carol", "pw"));

    createFinishedSession(alice, bob, GameState.WHITE_WINS); // Alice wins as WHITE
    createFinishedSession(bob, alice, GameState.WHITE_WINS); // Bob wins as WHITE
    createFinishedSession(alice, carol, GameState.TIE);
    createFinishedSession(bob, carol, GameState.BLACK_WINS); // Carol wins as BLACK
    createInProgressSession(alice, bob); // should not count

    Page<LeaderboardRow> page = leaderboardRepository.findLeaderboard(PageRequest.of(0, 10));
    List<LeaderboardRow> rows = page.getContent();

    assertEquals(3, rows.size());
    assertRow(rows.get(0), carol, 2, 1, 0, 1);
    assertRow(rows.get(1), alice, 3, 1, 1, 1);
    assertRow(rows.get(2), bob, 3, 1, 2, 0);
  }

  private void assertRow(LeaderboardRow row, User user, long games, long wins, long losses, long draws) {
    assertEquals(user.getId(), row.getUserId());
    assertEquals(user.getUsername(), row.getUsername());
    assertEquals(games, row.getGames());
    assertEquals(wins, row.getWins());
    assertEquals(losses, row.getLosses());
    assertEquals(draws, row.getDraws());
  }

  private GameSession createFinishedSession(User whiteUser, User blackUser, GameState gameState) {
    Player white = new Player(PlayerColor.WHITE);
    white.setAccount(whiteUser);
    Player black = new Player(PlayerColor.BLACK);
    black.setAccount(blackUser);

    GameSession session = new GameSession(new Board(8, 8), white, GameType.PLAYER_VS_PLAYER);
    session.joinSession(black);
    session.setGameState(gameState);
    return sessionRepository.save(session);
  }

  private GameSession createInProgressSession(User whiteUser, User blackUser) {
    Player white = new Player(PlayerColor.WHITE);
    white.setAccount(whiteUser);
    Player black = new Player(PlayerColor.BLACK);
    black.setAccount(blackUser);

    GameSession session = new GameSession(new Board(8, 8), white, GameType.PLAYER_VS_PLAYER);
    session.joinSession(black);
    return sessionRepository.save(session);
  }
}
