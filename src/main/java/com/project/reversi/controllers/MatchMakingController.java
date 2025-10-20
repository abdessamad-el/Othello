package com.project.reversi.controllers;

import com.project.reversi.dto.EnqueueRequestDTO;
import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.dto.MatchStatusDTO;
import com.project.reversi.model.MatchStatus;
import com.project.reversi.model.User;
import com.project.reversi.services.MatchMakingService;
import java.awt.Color;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchMakingController {

  private final MatchMakingService matchMakingService;

  public MatchMakingController(MatchMakingService matchMakingService) {
    this.matchMakingService = matchMakingService;
  }

  @GetMapping("/{ticketId}")
  public ResponseEntity<MatchStatusDTO> getMatchStatus(@PathVariable UUID ticketId) {
    MatchStatus status = matchMakingService.getStatus(ticketId);

    if (status == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    GameSessionSummaryDTO summary = null;
    if (status == MatchStatus.FOUND) {
      summary = matchMakingService.getSessionByTicketId(ticketId)
          .map(GameSessionSummaryDTO::fromGameSession)
          .orElse(null);
    }

    String assignedColor = matchMakingService.getAssignedColor(ticketId)
        .map(color -> Color.WHITE.equals(color) ? "WHITE" : "BLACK")
        .orElse(null);

    MatchStatusDTO dto = new MatchStatusDTO(status.name(), summary, assignedColor);
    return ResponseEntity.ok(dto);
  }

  @GetMapping("/auth-check")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<User> authCheck(@AuthenticationPrincipal User currentUser) {
    return ResponseEntity.ok(currentUser);
  }

  @PostMapping("/enqueue")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UUID> enqueue(@RequestBody EnqueueRequestDTO enqueueRequest,
                                      @AuthenticationPrincipal User currentUser) {
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    UUID ticketId = matchMakingService.enqueue(currentUser, enqueueRequest.preferredColor());
    return ResponseEntity.created(null).body(ticketId);
  }

  @DeleteMapping("/cancel/{ticketId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<Void> cancel(@PathVariable UUID ticketId) {
    boolean canceled = matchMakingService.cancel(ticketId);
    if (canceled) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }
}
