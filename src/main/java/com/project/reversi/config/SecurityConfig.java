package com.project.reversi.config;

import com.project.reversi.model.User;
import com.project.reversi.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {


  private final WebsocketProperties websocketProperties;

  public SecurityConfig(WebsocketProperties websocketProperties) {
    this.websocketProperties = websocketProperties;
  }


  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService userDetailsService(UserRepository userRepo) {
    return username -> {
      User user = userRepo.findByUsername(username);
      if (user != null) {
        return user;
      }
      throw new UsernameNotFoundException(
          "User '" + username + "' not found");
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    String websocketEndpoint = websocketProperties.getEndpoint();

    return http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeRequests(auth -> auth
            .antMatchers("/", "/index.html", "/style.css", "/script.js", "/favicon.ico", "/css/**", "/js/**", "/images/**",
                websocketEndpoint, websocketEndpoint + "/**").permitAll()
            .antMatchers("/api/auth/**").permitAll()
            .antMatchers("/h2-console/**").permitAll()
            .antMatchers(HttpMethod.POST, "/api/session/create").permitAll()
            .antMatchers(HttpMethod.GET, "/api/game/board").permitAll()
            .antMatchers(HttpMethod.GET, "/api/session/**").permitAll()
            .antMatchers("/api/game/possible-moves").permitAll()
            .antMatchers("/api/game/move").permitAll()
            .anyRequest().authenticated())
        .formLogin(form -> form
      .successHandler((request, response, authentication) -> {
        String target = request.getParameter("next");
        if (target != null && !target.isBlank()) {
          response.sendRedirect(target);
        } else {
          response.sendRedirect("/");
        }
      })
  )
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
        .headers(headers -> headers.frameOptions().sameOrigin())
        .build();
  }

}
