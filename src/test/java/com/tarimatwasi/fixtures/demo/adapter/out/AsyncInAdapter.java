package com.tarimatwasi.fixtures.demo.adapter.out;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

/** An output adapter may use {@code @Async} and {@code @EnableAsync} (BE-SPR-CON-01). */
@Configuration
@EnableAsync
public class AsyncInAdapter {

  @Async
  public void send() {}
}
