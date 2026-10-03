package com.tarimatwasi.fixtures.demo.application;

import org.springframework.scheduling.annotation.Async;

/** {@code @Async} outside {@code adapter.out} breaks BE-SPR-CON-01. */
public class AsyncInService {

  @Async
  public void run() {}
}
