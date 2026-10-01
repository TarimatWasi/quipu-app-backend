package com.tarimatwasi.quipu.shared.adapter.out.email;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ResendPingService {

  private final RestClient restClient;
  private final String fromEmail;

  public ResendPingService(
      @Value("${app.resend.api-key}") String apiKey,
      @Value("${app.resend.from-email}") String fromEmail) {
    this.restClient =
        RestClient.builder()
            .baseUrl("https://api.resend.com")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    this.fromEmail = fromEmail;
  }

  public String ping() {
    try {
      restClient
          .post()
          .uri("/emails")
          .body(
              Map.of(
                  "from",
                  fromEmail,
                  "to",
                  fromEmail,
                  "subject",
                  "TarimatWasi connectivity check",
                  "text",
                  "This is an automated connectivity check."))
          .retrieve()
          .toBodilessEntity();
      return "OK";
    } catch (RestClientException e) {
      return "FAIL: " + e.getMessage();
    }
  }
}
