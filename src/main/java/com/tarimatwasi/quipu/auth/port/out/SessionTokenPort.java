package com.tarimatwasi.quipu.auth.port.out;

/** Issues the session credential that the BFF hands to the browser. */
public interface SessionTokenPort {

  /**
   * Issues a session token for the account.
   *
   * @param mustChangePassword true while the account must change its password (RF-12): such a
   *     session can only reach the password change
   */
  String issue(String userId, String role, boolean mustChangePassword);
}
