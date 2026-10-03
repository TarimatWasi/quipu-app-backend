package com.tarimatwasi.quipu.auth.adapter.out.mail;

import com.tarimatwasi.quipu.auth.port.out.PasswordResetMailPort;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * The local profile has no email provider: the link is written in the console so that whoever
 * develops can open it and choose the password of the first ADMIN. Nothing leaves the machine.
 */
@Component
@Profile("local & !dev & !prod")
public class LoggingPasswordResetMail implements PasswordResetMailPort {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingPasswordResetMail.class);

  private final String frontendOrigin;

  public LoggingPasswordResetMail(@Value("${app.cors.allowed-origin}") String frontendOrigin) {
    this.frontendOrigin = frontendOrigin;
  }

  @Override
  public void sendResetLink(String toEmail, String code, Duration validFor) {
    LOG.info(
        "[local] Link for {} to choose a password (valid {} minutes): {}",
        toEmail,
        validFor.toMinutes(),
        ResetLink.of(frontendOrigin, code));
  }
}
