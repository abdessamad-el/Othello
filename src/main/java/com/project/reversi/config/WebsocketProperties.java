package com.project.reversi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration holder for websocket endpoints used throughout the application.
 */
@Component
@ConfigurationProperties(prefix = "reversi.websocket")
public class WebsocketProperties {

  /**
   * STOMP/SockJS endpoint for gameplay updates.
   */
  private String endpoint = "/ws/game";

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
}
