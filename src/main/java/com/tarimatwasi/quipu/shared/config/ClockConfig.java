package com.tarimatwasi.quipu.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Single time source (QP-SPRMONO-TIM-01): all business time is America/Lima. */
@Configuration(proxyBeanMethods = false)
class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.system(ZoneId.of("America/Lima"));
  }
}
