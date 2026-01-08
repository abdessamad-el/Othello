package com.project.reversi.controllers;


import com.project.reversi.dto.LeaderboardRow;
import com.project.reversi.model.User;
import com.project.reversi.repository.LeaderboardRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/leaderboard")
public class LeaderboardController {


  final LeaderboardRepository leaderboardRepository;

  public LeaderboardController(LeaderboardRepository leaderboardRepository) {this.leaderboardRepository = leaderboardRepository;}

  @GetMapping()
  public ResponseEntity<Page<LeaderboardRow>> getLeaderBoard(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    Page<LeaderboardRow> leaderboard = leaderboardRepository.findLeaderboard(PageRequest.of(page, size));
    return ResponseEntity.ok(leaderboard);
  }

  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<LeaderboardRow> getMyStats(@AuthenticationPrincipal User currentUser) {
    if (currentUser == null || currentUser.getId() == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    LeaderboardRow stats = leaderboardRepository.findStatsByUserId(currentUser.getId());
    if (stats == null) {
      return ResponseEntity.noContent().build();
    }
    return ResponseEntity.ok(stats);
  }
}
