package com.project.reversi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final WebsocketProperties websocketProperties;

  public SecurityConfig(WebsocketProperties websocketProperties) {
    this.websocketProperties = websocketProperties;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    String websocketEndpoint = websocketProperties.getEndpoint();

    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeRequests(auth -> auth
            .antMatchers("/", "/index.html", "/style.css", "/script.js", "/favicon.ico", "/css/**", "/js/**", "/images/**",
                websocketEndpoint, websocketEndpoint + "/**").permitAll()
            .antMatchers(HttpMethod.POST, "/api/session/create", "/api/session/*/join").permitAll()
            .antMatchers("/api/matchmaking/**").permitAll()
            .antMatchers(HttpMethod.GET, "/api/session/**").permitAll()
            .antMatchers("/api/game/possible-moves").permitAll()
            .antMatchers("/api/game/move").permitAll()
            .anyRequest().authenticated()
        );

    return http.build();
  }
}
