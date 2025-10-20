package com.project.reversi.controllers;

import com.project.reversi.model.User;
import com.project.reversi.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public RegistrationController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
    if (request == null
        || !StringUtils.hasText(request.username())
        || !StringUtils.hasText(request.password())) {
      return ResponseEntity.badRequest().body("Username and password are required.");
    }

    String normalizedUsername = request.username().trim();
    if (userRepository.findByUsername(normalizedUsername) != null) {
      return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists.");
    }

    User user = new User(normalizedUsername, passwordEncoder.encode(request.password()));
    userRepository.save(user);

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
