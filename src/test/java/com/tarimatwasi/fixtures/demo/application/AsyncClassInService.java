package com.tarimatwasi.fixtures.demo.application;

import org.springframework.scheduling.annotation.Async;

/** A class-level {@code @Async} outside {@code adapter.out} breaks BE-SPR-CON-01. */
@Async
public class AsyncClassInService {}
