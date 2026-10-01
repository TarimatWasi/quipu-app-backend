package com.tarimatwasi.quipu.auth.application;

import com.tarimatwasi.quipu.auth.domain.UserAccount;
import com.tarimatwasi.quipu.auth.port.in.LoginUseCase;
import com.tarimatwasi.quipu.auth.port.out.UserRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthApplicationService implements LoginUseCase {

  private static final String DUMMY_PASSWORD_HASH =
      "$2b$10$izcb2KSDHR.LLCnLSqUYZ.2cp1yucRUSMDq0Eo9HEg4LQaSzNfmEC";

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthApplicationService(
      UserRepositoryPort userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public LoginResult login(LoginCommand command) {
    var userOpt = userRepository.findByDocument(command.documentType(), command.documentNumber());

    UserAccount user = userOpt.orElse(null);
    String passwordHashToCheck = user != null ? user.passwordHash() : DUMMY_PASSWORD_HASH;

    // Always perform password check to prevent timing attacks
    if (!passwordEncoder.matches(command.rawPassword(), passwordHashToCheck)) {
      throw new InvalidCredentialsException();
    }

    if (user == null) {
      throw new InvalidCredentialsException();
    }

    if (user.isDisabled()) {
      throw new AccountDisabledException();
    }

    return new LoginResult(
        user.id().toString(), user.role(), user.email(), user.mustChangePassword());
  }
}
