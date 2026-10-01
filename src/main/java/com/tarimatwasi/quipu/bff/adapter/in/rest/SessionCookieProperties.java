package com.tarimatwasi.quipu.bff.adapter.in.rest;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Attributes of the session cookie that are not fixed (Secure, HttpOnly and Path always are).
 *
 * @param sameSite {@code app.session.same-site}: Lax by default (frontend and BFF share the origin
 *     through the hosting rewrite, TAR-75); a product with the frontend on another registrable
 *     domain chooses {@code none}
 * @param maxAge {@code app.session.max-age}: the base configuration derives it from the token
 *     lifetime ({@code app.jwt.expiration-minutes}) so that cookie and token expire together
 */
@ConfigurationProperties("app.session")
@Validated
public record SessionCookieProperties(
    @DefaultValue("lax") SameSite sameSite, @DefaultValue("8h") Duration maxAge) {

  /** Values of the SameSite attribute. */
  public enum SameSite {
    STRICT("Strict"),
    LAX("Lax"),
    NONE("None");

    private final String attribute;

    SameSite(String attribute) {
      this.attribute = attribute;
    }

    String attribute() {
      return attribute;
    }
  }
}
