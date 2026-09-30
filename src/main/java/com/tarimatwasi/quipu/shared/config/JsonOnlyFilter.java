package com.tarimatwasi.quipu.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF mitigation of ADR-006 (QP-SPRMONO-CSR-01): a state-changing request that is not {@code
 * application/json} is rejected with 415 before it reaches authorization.
 */
final class JsonOnlyFilter extends OncePerRequestFilter {

  private static final Set<String> STATE_CHANGING = Set.of("POST", "PUT", "PATCH", "DELETE");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (STATE_CHANGING.contains(request.getMethod()) && !isJson(request.getContentType())) {
      // setStatus, not sendError: sendError triggers an ERROR dispatch to /error, which the
      // security chain would answer with 401 for an unauthenticated client.
      response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
      return;
    }
    chain.doFilter(request, response);
  }

  private static boolean isJson(String contentType) {
    if (contentType == null) {
      return false;
    }
    try {
      return MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType));
    } catch (InvalidMediaTypeException e) {
      return false;
    }
  }
}
