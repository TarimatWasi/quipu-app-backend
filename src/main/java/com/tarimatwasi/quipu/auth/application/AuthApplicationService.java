package com.tarimatwasi.quipu.auth.application;

import com.tarimatwasi.quipu.auth.domain.UserAccount;
import com.tarimatwasi.quipu.auth.port.in.ChangePasswordUseCase;
import com.tarimatwasi.quipu.auth.port.in.LoginUseCase;
import com.tarimatwasi.quipu.auth.port.in.PasswordUnchangedException;
import com.tarimatwasi.quipu.auth.port.in.WeakPasswordException;
import com.tarimatwasi.quipu.auth.port.out.SessionTokenPort;
import com.tarimatwasi.quipu.auth.port.out.UserRepositoryPort;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService implements LoginUseCase, ChangePasswordUseCase {

  private static final String DUMMY_PASSWORD_HASH =
      "$2b$10$izcb2KSDHR.LLCnLSqUYZ.2cp1yucRUSMDq0Eo9HEg4LQaSzNfmEC";
  private static final int MIN_PASSWORD_LENGTH = 8;
  private static final int MAX_PASSWORD_BYTES = 72;

  private final UserRepositoryPort userRepository;
  private final PasswordEncoder passwordEncoder;
  private final SessionTokenPort sessionTokens;

  public AuthApplicationService(
      UserRepositoryPort userRepository,
      PasswordEncoder passwordEncoder,
      SessionTokenPort sessionTokens) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.sessionTokens = sessionTokens;
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

  @Override
  @Transactional
  public ChangePasswordResult changePassword(ChangePasswordCommand command) {
    UserAccount user = findAccount(command.userId());
    if (user.isDisabled()) {
      throw new AccountDisabledException();
    }
    requireCurrentPassword(user, command.currentPassword());
    requireStrongPassword(command.newPassword());
    if (passwordEncoder.matches(command.newPassword(), user.passwordHash())) {
      throw new PasswordUnchangedException();
    }
    userRepository.changePassword(user.id(), passwordEncoder.encode(command.newPassword()));
    return new ChangePasswordResult(
        sessionTokens.issue(command.userId(), user.role().name(), false));
  }

  /** The id comes from the session, but a malformed or unknown one is just another bad session. */
  private UserAccount findAccount(String userId) {
    try {
      return userRepository
          .findById(UUID.fromString(userId))
          .orElseThrow(InvalidCredentialsException::new);
    } catch (IllegalArgumentException e) {
      throw new InvalidCredentialsException();
    }
  }

  /** Optional only while the account must change its password: that session just logged in. */
  private void requireCurrentPassword(UserAccount user, @Nullable String currentPassword) {
    boolean matches =
        currentPassword != null && passwordEncoder.matches(currentPassword, user.passwordHash());
    boolean skippable = currentPassword == null && user.mustChangePassword();
    if (!matches && !skippable) {
      throw new InvalidCredentialsException();
    }
  }

  private static void requireStrongPassword(String password) {
    if (password.codePointCount(0, password.length()) < MIN_PASSWORD_LENGTH) {
      throw new WeakPasswordException(WeakPasswordException.Reason.TOO_SHORT);
    }
    if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
      throw new WeakPasswordException(WeakPasswordException.Reason.TOO_LONG);
    }
  }
}
