package com.tarimatwasi.quipu.auth.adapter.out.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final SecretKey key;
  private final long expirationMinutes;

  public JwtTokenProvider(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMinutes = expirationMinutes;
  }

  public String issue(String userId, String role) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(userId)
        .claim("role", role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  public record Session(String userId, String role) {}

  /** Empty if the token is malformed, tampered with, or expired. */
  public Optional<Session> parse(String token) {
    try {
      var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
      String userId = claims.getSubject();
      String role = claims.get("role", String.class);
      if (userId == null || userId.isBlank() || role == null || role.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(new Session(userId, role));
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }
}
