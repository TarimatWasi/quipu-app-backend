package com.tarimatwasi.fixtures.demo.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** {@code @EnableAsync} outside {@code adapter.out} breaks BE-SPR-CON-01. */
@Configuration
@EnableAsync
public class EnableAsyncInService {}
