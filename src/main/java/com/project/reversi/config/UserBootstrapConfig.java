package com.project.reversi.config;

import com.project.reversi.model.User;
import com.project.reversi.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserBootstrapConfig {

  private static final Logger log = LoggerFactory.getLogger(UserBootstrapConfig.class);

  @Bean
  CommandLineRunner createDefaultUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return args -> {
      List<DefaultUser> defaults = List.of(
          new DefaultUser("alice", "password"),
          new DefaultUser("bob", "password"),
          new DefaultUser("charlie", "password"));

      for (DefaultUser defaultUser : defaults) {
        if (userRepository.findByUsername(defaultUser.username()) != null) {
          continue;
        }
        User user = new User(defaultUser.username(), passwordEncoder.encode(defaultUser.rawPassword()));
        userRepository.save(user);
        log.info("Created default user '{}'", defaultUser.username());
      }
    };
  }

  private record DefaultUser(String username, String rawPassword) {}
}
