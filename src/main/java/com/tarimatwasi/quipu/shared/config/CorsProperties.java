package com.tarimatwasi.quipu.shared.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Extra origins allowed by CORS besides the primary one ({@code app.cors.allowed-origin}).
 *
 * <p>Behind a rewrite of the hosting layer (TAR-75) the browser keeps the {@code Origin} of the
 * frontend host while the {@code Host} is the backend, so Spring sees a cross-origin request: the
 * hosts of the frontend (its aliases, branch and deployment hosts) must be listed.
 *
 * @param allowedOrigins exact origins ({@code app.cors.allowed-origins})
 * @param allowedOriginPatterns origins with a wildcard inside the first label only ({@code
 *     app.cors.allowed-origin-patterns}), matched in full by Spring
 */
@ConfigurationProperties("app.cors")
@Validated
public record CorsProperties(List<String> allowedOrigins, List<String> allowedOriginPatterns) {

  public CorsProperties {
    allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    allowedOriginPatterns =
        allowedOriginPatterns == null ? List.of() : List.copyOf(allowedOriginPatterns);
  }

  /**
   * BE-SPR-SEC-05: validates every origin at startup and builds the CORS policy.
   *
   * @param primaryOrigin the required origin ({@code app.cors.allowed-origin})
   * @param requireHttps true in the deployed profiles (dev and prod)
   */
  public CorsConfiguration configuration(String primaryOrigin, boolean requireHttps) {
    SecurityConfig.validateOrigin(primaryOrigin, requireHttps);
    allowedOrigins.forEach(
        origin -> SecurityConfig.validateOrigin(origin, requireHttps, "app.cors.allowed-origins"));
    allowedOriginPatterns.forEach(SecurityConfig::validateOriginPattern);

    var origins = new ArrayList<String>();
    origins.add(primaryOrigin);
    origins.addAll(allowedOrigins);
    var config = new CorsConfiguration();
    config.setAllowedOrigins(origins);
    config.setAllowedOriginPatterns(allowedOriginPatterns);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    config.setAllowedHeaders(List.of("Content-Type"));
    config.setAllowCredentials(true);
    return config;
  }
}
