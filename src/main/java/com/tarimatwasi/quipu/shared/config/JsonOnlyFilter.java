package com.tarimatwasi.quipu.shared.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CSRF mitigation of ADR-006 (QP-SPRMONO-CSR-01): a state-changing request that carries a body or a
 * {@code Content-Type} must be {@code application/json}; otherwise it is rejected with 415 before
 * it reaches authorization.
 *
 * <p>A request with neither body nor {@code Content-Type} (a bodyless DELETE or action POST) is let
 * through: a cross-site one still carries an {@code Origin} header, which the exact-origin CORS
 * configuration rejects with 403 before this filter runs.
 */
final class JsonOnlyFilter extends OncePerRequestFilter {

  private static final Set<String> STATE_CHANGING = Set.of("POST", "PUT", "PATCH", "DELETE");

  /** BFF error shape (QP-SPRMONO-BFF-01): stable English code, Spanish message. */
  private static final String BODY =
      "{\"code\":\"UNSUPPORTED_MEDIA_TYPE\",\"message\":\"El contenido debe ser application/json.\"}";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (STATE_CHANGING.contains(request.getMethod())
        && (hasBody(request) || request.getContentType() != null)
        && !isJson(request.getContentType())) {
      // Write the response directly: sendError triggers an ERROR dispatch to /error, which the
      // security chain would answer with 401 for an unauthenticated client.
      response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response.getWriter().write(BODY);
      return;
    }
    chain.doFilter(request, response);
  }

  private static boolean hasBody(HttpServletRequest request) {
    return request.getContentLengthLong() > 0
        || request.getHeader(HttpHeaders.TRANSFER_ENCODING) != null;
  }

  private static boolean isJson(String contentType) {
    if (contentType == null) {
      return false;
    }
    try {
      // equalsTypeAndSubtype: parameters such as charset are allowed
      return MediaType.APPLICATION_JSON.equalsTypeAndSubtype(MediaType.parseMediaType(contentType));
    } catch (InvalidMediaTypeException e) {
      return false;
    }
  }
}
