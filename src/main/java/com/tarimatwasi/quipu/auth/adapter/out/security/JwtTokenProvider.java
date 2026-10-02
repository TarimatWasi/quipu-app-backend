package com.tarimatwasi.quipu.auth.adapter.out.security;

import com.tarimatwasi.quipu.auth.port.out.SessionTokenPort;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements SessionTokenPort {

  /** Present (true) only while the account must change its password (RF-12). */
  private static final String PASSWORD_CHANGE_PENDING_CLAIM = "mcp";

  private final SecretKey key;
  private final long expirationMinutes;
  private final Clock clock;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
      Clock clock) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = expirationMinutes;
    this.clock = clock;
  }

  public String issue(String userId, String role) {
    return issue(userId, role, false);
  }

  @Override
  public String issue(String userId, String role, boolean mustChangePassword) {
    Instant now = clock.instant();
    return Jwts.builder()
        .subject(userId)
        .claim("role", role)
        .claim(PASSWORD_CHANGE_PENDING_CLAIM, mustChangePassword ? Boolean.TRUE : null)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public record Session(String userId, String role, boolean mustChangePassword) {}

  /** Empty if the token is malformed, tampered with, or expired. */
  public Optional<Session> parse(String token) {
    try {
      var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      String userId = claims.getSubject();
      String role = claims.get("role", String.class);
      if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
        return Optional.empty();
      }
      boolean mustChangePassword =
          Boolean.TRUE.equals(claims.get(PASSWORD_CHANGE_PENDING_CLAIM, Boolean.class));
      return Optional.of(new Session(userId, role, mustChangePassword));
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
