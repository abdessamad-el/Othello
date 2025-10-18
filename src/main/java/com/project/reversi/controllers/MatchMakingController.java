package com.project.reversi.controllers;
import org.springframework.web.bind.annotation.*;
import com.project.reversi.dto.EnqueueRequestDTO;
import com.project.reversi.dto.GameSessionSummaryDTO;
import com.project.reversi.dto.MatchStatusDTO;
import com.project.reversi.model.MatchStatus;
import com.project.reversi.services.MatchMakingService;

import java.awt.Color;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


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


  @PostMapping("/enqueue")
  public ResponseEntity<UUID> enqueue(@RequestBody EnqueueRequestDTO enqueueRequest) {
    var ticketId = matchMakingService.enqueue(enqueueRequest.nickName(), enqueueRequest.preferredColor());


    return ResponseEntity.created(null).body(ticketId);
  }

  @DeleteMapping("/cancel/{ticketId}")
  public ResponseEntity<Void> cancel(@PathVariable UUID ticketId) {
    boolean canceled = matchMakingService.cancel(ticketId);
    if (canceled) {
      return ResponseEntity.noContent().build();
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
  }
}
