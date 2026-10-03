package com.tarimatwasi.fixtures.demo.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Scheduled tasks need an ADR: {@code @EnableScheduling} breaks BE-SPR-CON-01 anywhere. */
@Configuration
@EnableScheduling
public class SchedulingEnabled {}
