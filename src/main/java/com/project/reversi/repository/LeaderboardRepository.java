package com.project.reversi.repository;

import com.project.reversi.dto.LeaderboardRow;
import com.project.reversi.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface LeaderboardRepository extends Repository<User, Long> {

  String LEADERBOARD_BASE = """
      SELECT *
      FROM (
               SELECT
                   leaderboard.*,
                   RANK() OVER (
                       ORDER BY wins DESC,
                                winRate DESC,
                                losses ASC
                   ) AS rank
               FROM (
                        SELECT
                            u.id AS userId,
                            u.username AS username,

                            COUNT(*) AS games,

                            SUM(CASE
                                    WHEN (p.color = 'BLACK' AND gs.game_state = 'BLACK_WINS')
                                        OR (p.color = 'WHITE' AND gs.game_state = 'WHITE_WINS')
                                        THEN 1 ELSE 0 END) AS wins,

                            SUM(CASE
                                    WHEN (p.color = 'BLACK' AND gs.game_state = 'WHITE_WINS')
                                        OR (p.color = 'WHITE' AND gs.game_state = 'BLACK_WINS')
                                        THEN 1 ELSE 0 END) AS losses,

                            SUM(CASE WHEN gs.game_state = 'TIE' THEN 1 ELSE 0 END) AS draws,

                            SUM(CASE
                                    WHEN (p.color = 'BLACK' AND gs.game_state = 'BLACK_WINS')
                                        OR (p.color = 'WHITE' AND gs.game_state = 'WHITE_WINS')
                                        THEN 1 ELSE 0 END) * 1.0 / NULLIF(COUNT(*), 0) AS winRate
                        FROM app_user u
                                 JOIN player p ON u.id = p.user_id
                                 JOIN game_session gs ON p.session_id = gs.session_id
                        WHERE gs.game_type = 'PLAYER_VS_PLAYER'
                          AND gs.game_state <> 'IN_PROGRESS'
                        GROUP BY u.id, u.username
                    ) leaderboard
           ) ranked
      """;

  String LEADERBOARD_ORDER = """
      ORDER BY
          wins DESC,
          winRate DESC,
          losses ASC
      """;

  String LEADERBOARD_COUNT = """
      SELECT COUNT(*)
      FROM (
               SELECT u.id
               FROM app_user u
                        JOIN player p ON u.id = p.user_id
                        JOIN game_session gs ON p.session_id = gs.session_id
               WHERE gs.game_type = 'PLAYER_VS_PLAYER'
                 AND gs.game_state <> 'IN_PROGRESS'
               GROUP BY u.id
           ) leaderboard
      """;

  @Query(
      value = LEADERBOARD_BASE + LEADERBOARD_ORDER,
      countQuery = LEADERBOARD_COUNT,
      nativeQuery = true
  )
  Page<LeaderboardRow> findLeaderboard(Pageable pageable);

  @Query(
      value = LEADERBOARD_BASE + " WHERE userId = :userId",
      nativeQuery = true
  )
  LeaderboardRow findStatsByUserId(@Param("userId") Long userId);

}
