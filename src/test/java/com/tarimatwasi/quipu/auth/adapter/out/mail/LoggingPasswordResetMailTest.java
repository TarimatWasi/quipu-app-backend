package com.tarimatwasi.quipu.auth.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class LoggingPasswordResetMailTest {

  @Test
  void writesTheLinkToTheConsoleAndSendsNothing() {
    var logger = (Logger) LoggerFactory.getLogger(LoggingPasswordResetMail.class);
    var appender = new ListAppender<ILoggingEvent>();
    appender.start();
    logger.addAppender(appender);
    try {
      new LoggingPasswordResetMail("http://localhost:4200/")
          .sendResetLink("admin@quipu.local", "CODE123", Duration.ofMinutes(30));
    } finally {
      logger.detachAppender(appender);
    }

    assertThat(appender.list)
        .singleElement()
        .extracting(ILoggingEvent::getFormattedMessage)
        .asString()
        .contains("http://localhost:4200/reset-password?code=CODE123")
        .contains("admin@quipu.local");
  }
}
