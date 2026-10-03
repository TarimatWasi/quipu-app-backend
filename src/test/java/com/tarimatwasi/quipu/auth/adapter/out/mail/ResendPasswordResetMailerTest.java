package com.tarimatwasi.quipu.auth.adapter.out.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.tarimatwasi.quipu.auth.port.out.MailDeliveryException;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResendPasswordResetMailerTest {

  private static final String CODE = "abc-DEF_123";

  private final RestClient.Builder builder = RestClient.builder();
  private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
  private final ResendPasswordResetMailer mailer =
      new ResendPasswordResetMailer(
          builder, "re_test_key", "Quipu <no-reply@example.test>", "https://app.example.test");

  @Test
  void sendsTheLinkToTheFrontendInSpanishWithTheValidity() {
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("Authorization", "Bearer re_test_key"))
        .andExpect(jsonPath("$.from").value("Quipu <no-reply@example.test>"))
        .andExpect(jsonPath("$.to").value("guest@example.test"))
        .andExpect(jsonPath("$.subject").value("Restablece tu contraseña de Quipu"))
        .andExpect(
            request -> {
              String body = ((MockClientHttpRequest) request).getBodyAsString();
              assertThat(body)
                  .contains("https://app.example.test/reset-password?code=" + CODE)
                  .contains("30 minutos");
            })
        .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

    mailer.sendResetLink("guest@example.test", CODE, Duration.ofMinutes(30));

    server.verify();
  }

  @Test
  void aTrailingSlashInTheOriginDoesNotDoubleTheSlash() {
    var otherBuilder = RestClient.builder();
    var otherServer = MockRestServiceServer.bindTo(otherBuilder).build();
    var other =
        new ResendPasswordResetMailer(
            otherBuilder, "re_test_key", "no-reply@example.test", "https://app.example.test/");
    otherServer
        .expect(requestTo("https://api.resend.com/emails"))
        .andExpect(
            request ->
                assertThat(((MockClientHttpRequest) request).getBodyAsString())
                    .contains("https://app.example.test/reset-password?code=" + CODE))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    other.sendResetLink("guest@example.test", CODE, Duration.ofMinutes(30));

    otherServer.verify();
  }

  @Test
  void aConnectionFailureBecomesAMailDeliveryException() {
    server
        .expect(requestTo("https://api.resend.com/emails"))
        .andRespond(withException(new IOException("connection refused")));

    assertThatThrownBy(
            () -> mailer.sendResetLink("guest@example.test", CODE, Duration.ofMinutes(30)))
        .isInstanceOf(MailDeliveryException.class);
  }

  @Test
  void aRejectedEmailBecomesAMailDeliveryExceptionWithoutTheAddressOrTheCode() {
    server.expect(requestTo("https://api.resend.com/emails")).andRespond(withServerError());

    assertThatThrownBy(
            () -> mailer.sendResetLink("guest@example.test", CODE, Duration.ofMinutes(30)))
        .isInstanceOf(MailDeliveryException.class)
        .satisfies(
            e ->
                assertThat(e.getMessage())
                    .doesNotContain("guest@example.test")
                    .doesNotContain(CODE));
  }
}
