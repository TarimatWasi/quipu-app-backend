package com.tarimatwasi.quipu.auth.adapter.out.security;

import com.tarimatwasi.quipu.auth.port.out.SessionTokenPort;
import com.tarimatwasi.quipu.auth.port.out.UserRepositoryPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider implements SessionTokenPort {

  /**
   * Always written: true while the account must change its password (RF-12). A token without it was
   * issued before the mark existed, so the database decides.
   */
  private static final String PASSWORD_CHANGE_PENDING_CLAIM = "mcp";

  private final SecretKey key;
  private final long expirationMinutes;
  private final Clock clock;
  private final UserRepositoryPort users;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
      Clock clock,
      UserRepositoryPort users) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = expirationMinutes;
    this.clock = clock;
    this.users = users;
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
        .claim(PASSWORD_CHANGE_PENDING_CLAIM, mustChangePassword)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public record Session(String userId, String role, boolean mustChangePassword) {}

  /** Empty if the token is malformed, tampered with, or expired. */
  public Optional<Session> parse(String token) {
    Claims claims;
    try {
      claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
    String userId = claims.getSubject();
    String role = claims.get("role", String.class);
    if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
      return Optional.empty();
    }
    Boolean marked = claims.get(PASSWORD_CHANGE_PENDING_CLAIM, Boolean.class);
    Optional<Boolean> pending = marked != null ? Optional.of(marked) : pendingInDatabase(userId);
    return pending.map(mustChange -> new Session(userId, role, mustChange));
  }

  /** Token from before the mark: the persisted account decides; an unknown account is rejected. */
  private Optional<Boolean> pendingInDatabase(String userId) {
    UUID id;
    try {
      id = UUID.fromString(userId);
    } catch (IllegalArgumentException notAnId) {
      return Optional.empty();
    }
    return users.findById(id).map(account -> account.mustChangePassword());
  }
}
