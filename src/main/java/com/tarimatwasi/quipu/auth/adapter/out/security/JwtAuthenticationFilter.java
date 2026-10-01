package com.tarimatwasi.quipu.auth.adapter.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates the request from the sessionToken cookie; an invalid token just leaves it
 * anonymous.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  static final String COOKIE_NAME = "sessionToken";

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
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      Arrays.stream(cookies)
          .filter(c -> COOKIE_NAME.equals(c.getName()))
          .findFirst()
          .flatMap(c -> jwtTokenProvider.parse(c.getValue()))
          .ifPresent(
              session ->
                  SecurityContextHolder.getContext()
                      .setAuthentication(
                          new UsernamePasswordAuthenticationToken(
                              session.userId(),
                              null,
                              List.of(new SimpleGrantedAuthority("ROLE_" + session.role())))));
    }
    chain.doFilter(request, response);
  }
}
