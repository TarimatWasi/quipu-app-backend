package com.tarimatwasi.quipu.auth.adapter.out.mail;

import com.tarimatwasi.quipu.auth.port.out.MailDeliveryException;
import com.tarimatwasi.quipu.auth.port.out.PasswordResetMailPort;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Sends the recovery email through Resend (free plan). The link points to the frontend, whose
 * origin is the one CORS already trusts ({@code app.cors.allowed-origin}).
 */
@Component
public class ResendPasswordResetMailer implements PasswordResetMailPort {

  private final RestClient restClient;
  private final String fromEmail;
  private final String frontendOrigin;

  @Autowired
  public ResendPasswordResetMailer(
      ResendProperties resend, @Value("${app.cors.allowed-origin}") String frontendOrigin) {
    this(
        RestClient.builder().requestFactory(withTimeouts()),
        resend.apiKey(),
        resend.fromEmail(),
        frontendOrigin);
  }

  /** The tests bind a mock server to the builder. */
  ResendPasswordResetMailer(
      RestClient.Builder builder, String apiKey, String fromEmail, String frontendOrigin) {
    this.restClient =
        builder
            .baseUrl("https://api.resend.com")
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
    this.fromEmail = fromEmail;
    this.frontendOrigin = frontendOrigin.replaceAll("/+$", "");
  }

  /**
   * A provider that hangs must not hold a request thread for long: the call happens while a person
   * waits for the answer of the recovery form.
   */
  private static ClientHttpRequestFactory withTimeouts() {
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    return factory;
  }

  @Override
  public void sendResetLink(String toEmail, String code, Duration validFor) {
    String link = frontendOrigin + "/reset-password?code=" + code;
    String text =
        "Hola,\n\n"
            + "Recibimos una solicitud para restablecer tu contraseña de Quipu. Abre este enlace"
            + " para elegir una nueva; vale "
            + validFor.toMinutes()
            + " minutos y solo se puede usar una vez:\n\n"
            + link
            + "\n\n"
            + "Si no lo pediste, ignora este correo: tu contraseña no cambia.\n";
    try {
      restClient
          .post()
          .uri("/emails")
          .body(
              Map.of(
                  "from",
                  fromEmail,
                  "to",
                  toEmail,
                  "subject",
                  "Restablece tu contraseña de Quipu",
                  "text",
                  text))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException | IllegalArgumentException e) {
      // The message carries neither the address nor the code: it may reach the logs.
      throw new MailDeliveryException("Resend did not accept the recovery email", e);
    }
  }
}
