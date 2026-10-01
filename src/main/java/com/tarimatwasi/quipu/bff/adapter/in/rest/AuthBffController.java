package com.tarimatwasi.quipu.bff.adapter.in.rest;

import com.tarimatwasi.quipu.auth.adapter.out.security.JwtTokenProvider;
import com.tarimatwasi.quipu.auth.domain.DocumentType;
import com.tarimatwasi.quipu.auth.port.in.LoginUseCase;
import com.tarimatwasi.quipu.auth.port.in.LoginUseCase.LoginCommand;
import com.tarimatwasi.quipu.auth.port.in.LoginUseCase.LoginResult;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthBffController {

  private final LoginUseCase loginUseCase;
  private final JwtTokenProvider jwtTokenProvider;

  public AuthBffController(LoginUseCase loginUseCase, JwtTokenProvider jwtTokenProvider) {
    this.loginUseCase = loginUseCase;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  public record LoginRequest(DocumentType documentType, String documentNumber, String password) {}

  public record LoginResponse(String role, String name, boolean mustChangePassword) {}

  @PostMapping("/bff/auth/login")
  public ResponseEntity<LoginResponse> login(
      @RequestBody LoginRequest request, HttpServletResponse response) {
    LoginResult result =
        loginUseCase.login(
            new LoginCommand(request.documentType(), request.documentNumber(), request.password()));

    String token = jwtTokenProvider.issue(result.userId(), result.role().name());
    ResponseCookie cookie =
        ResponseCookie.from("sessionToken", token)
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .build();
    response.addHeader("Set-Cookie", cookie.toString());

    return ResponseEntity.ok(
        new LoginResponse(
            result.role().name(), result.displayEmail(), result.mustChangePassword()));
  }
}
