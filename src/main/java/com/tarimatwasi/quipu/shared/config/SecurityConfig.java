package com.tarimatwasi.quipu.shared.config;

import com.tarimatwasi.quipu.auth.adapter.out.security.JwtAuthenticationFilter;
import com.tarimatwasi.quipu.auth.adapter.out.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** HTTP security baseline (BE-SPR-SEC-01, SEC-04, SEC-05, SEC-09). */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider)
      throws Exception {
    // CSRF is disabled on purpose (ADR-006). Mitigation, in layers: the session cookie is
    // SameSite=Lax (the browser talks to the frontend origin and the hosting layer rewrites
    // /bff, TAR-75); JsonOnlyFilter rejects any request with a body that is not JSON (415);
    // bodyless requests pass it and rely on CORS rejecting a foreign Origin (an allow-list of
    // exact origins and anchored patterns, see CorsProperties).
    http.csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults()) // uses the corsConfigurationSource bean
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'")))
        .exceptionHandling(
            e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .authorizeHttpRequests(
            a ->
                a.requestMatchers(
                        "/actuator/health", "/actuator/health/**", "/bff/auth/login", "/error")
                    .permitAll()
                    .requestMatchers("/bff/diagnostics/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(new JsonOnlyFilter(), AuthorizationFilter.class)
        .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), AuthorizationFilter.class);
    return http.build();
  }

  /**
   * The primary origin is required; {@link CorsProperties} adds the others. The deployed profiles
   * (dev and prod) accept https origins only.
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origin}") String allowedOrigin,
      CorsProperties cors,
      Environment environment) {
    boolean deployed = environment.acceptsProfiles(Profiles.of("dev", "prod"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cors.configuration(allowedOrigin, deployed));
    return source;
  }

  static void validateOrigin(String origin) {
    validateOrigin(origin, false);
  }

  static void validateOrigin(String origin, boolean requireHttps) {
    validateOrigin(origin, requireHttps, "app.cors.allowed-origin");
  }

  /** BE-SPR-SEC-05: an exact origin (scheme and host, no wildcard and no trailing slash). */
  static void validateOrigin(String origin, boolean requireHttps, String property) {
    if (!origin.matches("https?://[^/*\\s]+")) {
      throw new IllegalStateException(
          property
              + " must be an exact origin such as https://app.example.com"
              + " (no wildcard, path or trailing slash)");
    }
    if (requireHttps && !origin.startsWith("https://")) {
      throw new IllegalStateException(
          property + " must use https in the dev and prod profiles: " + origin);
    }
  }

  /**
   * BE-SPR-SEC-05: a pattern keeps a literal prefix of at least 8 characters in the first label and
   * a literal domain, and allows the wildcard only inside that first label ({@code
   * https://app-*-team.example.com}). Spring matches the whole origin against the pattern.
   *
   * <p>Ceiling: {@code *} also matches dots, so a host such as {@code
   * app-x.evil.com-team.example.com} would match; only the owner of {@code example.com} can create
   * hosts under it.
   */
  static void validateOriginPattern(String pattern) {
    if (!pattern.matches("https://[a-z0-9-]{8,}[a-z0-9*-]*(?:\\.[a-z0-9-]+)+")) {
      throw new IllegalStateException(
          "app.cors.allowed-origin-patterns must be https with a wildcard only inside the first"
              + " label after a literal prefix of 8 or more characters: "
              + pattern);
    }
  }

  /** No users yet: avoids Boot's generated default user (and its logged password). */
  @Bean
  UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager();
  }
}
