package com.project.reversi;

import com.project.reversi.services.AdvancedHeuristicMinMaxStrat;
import com.project.reversi.services.ComputerStrategy;
import com.project.reversi.services.MinMaxAlphaBetaStrat;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReversiApplication {
  public static void main(String[] args) {
    SpringApplication.run(ReversiApplication.class, args);
  }

  @Bean
  ComputerStrategy strategy(){
    return new MinMaxAlphaBetaStrat(6);
  }
}
