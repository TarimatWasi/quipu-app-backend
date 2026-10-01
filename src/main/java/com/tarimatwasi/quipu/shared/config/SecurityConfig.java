package com.tarimatwasi.quipu.shared.config;

import com.tarimatwasi.quipu.auth.adapter.out.security.JwtAuthenticationFilter;
import com.tarimatwasi.quipu.auth.adapter.out.security.JwtTokenProvider;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** HTTP security baseline (BE-SPR-SEC-01, SEC-04, SEC-05, SEC-09). */
@Configuration(proxyBeanMethods = false)
class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider)
      throws Exception {
    http.csrf(csrf -> csrf.disable()) // compensated by JsonOnlyFilter and exact CORS (ADR-006)
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

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origin}") String allowedOrigin) {
    validateOrigin(allowedOrigin);
    var config = new CorsConfiguration();
    config.setAllowedOrigins(List.of(allowedOrigin));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
    config.setAllowedHeaders(List.of("Content-Type"));
    config.setAllowCredentials(true);
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  /** BE-SPR-SEC-05: one exact origin (scheme and host, no wildcard and no trailing slash). */
  static void validateOrigin(String origin) {
    if (!origin.matches("https?://[^/*\\s]+")) {
      throw new IllegalStateException(
          "app.cors.allowed-origin must be one exact origin such as https://app.example.com"
              + " (no wildcard, path or trailing slash)");
    }
  }

  /** No users yet: avoids Boot's generated default user (and its logged password). */
  @Bean
  UserDetailsService userDetailsService() {
    return new InMemoryUserDetailsManager();
  }
}
