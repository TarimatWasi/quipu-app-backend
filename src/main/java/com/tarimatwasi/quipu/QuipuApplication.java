package com.tarimatwasi.quipu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.modulith.Modulithic;

/** Entry point of the Quipu modular monolith. */
@SpringBootApplication
@ConfigurationPropertiesScan
@Modulithic(sharedModules = "shared")
public class QuipuApplication {

  public static void main(String[] args) {
    SpringApplication.run(QuipuApplication.class, args);
  }
}
