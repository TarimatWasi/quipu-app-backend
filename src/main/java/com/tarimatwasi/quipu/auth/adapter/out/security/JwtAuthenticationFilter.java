package com.tarimatwasi.quipu.auth.adapter.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the request from the sessionToken cookie; an invalid token just leaves it
 * anonymous. While the session is marked as pending a password change (RF-12) it also answers 403
 * AUTH_PASSWORD_CHANGE_REQUIRED to everything except changing the password, reading the session and
 * leaving, so the change cannot be skipped by calling the API directly.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  static final String COOKIE_NAME = "sessionToken";

  /** What a session pending a password change may still reach. */
  private static final Set<String> ALLOWED_WHILE_PENDING =
      Set.of("/bff/auth/login", "/bff/auth/change-password", "/bff/auth/me", "/bff/auth/logout");

  // The body is written by hand: this module cannot depend on the BFF's error type.
  private static final String PASSWORD_CHANGE_REQUIRED_BODY =
      "{\"code\":\"AUTH_PASSWORD_CHANGE_REQUIRED\","
          + "\"message\":\"Debes cambiar tu contraseña para continuar\"}";

  private final JwtTokenProvider jwtTokenProvider;

  // Not a @Component: SecurityConfig adds it to the chain, avoiding a second servlet-level
  // registration.
  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Optional<JwtTokenProvider.Session> session = sessionOf(request);
    session.ifPresent(
        s ->
            SecurityContextHolder.getContext()
                .setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        s.userId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + s.role())))));
    if (session.filter(JwtTokenProvider.Session::mustChangePassword).isPresent()
        && !ALLOWED_WHILE_PENDING.contains(pathOf(request))) {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(PASSWORD_CHANGE_REQUIRED_BODY);
      return;
    }
    chain.doFilter(request, response);
  }

  private Optional<JwtTokenProvider.Session> sessionOf(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return Arrays.stream(cookies)
        .filter(c -> COOKIE_NAME.equals(c.getName()))
        .findFirst()
        .flatMap(c -> jwtTokenProvider.parse(c.getValue()));
  }

  private static String pathOf(HttpServletRequest request) {
    return request.getRequestURI().substring(request.getContextPath().length());
  }
}
