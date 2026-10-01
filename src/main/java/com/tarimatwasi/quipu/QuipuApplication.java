package com.tarimatwasi.quipu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.modulith.Modulithic;

/** Entry point of the Quipu modular monolith. */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
@Modulithic(sharedModules = "shared")
public class QuipuApplication {

  public static void main(String[] args) {
    SpringApplication.run(QuipuApplication.class, args);
  }
}
