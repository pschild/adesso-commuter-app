package de.pschild.adessocommutingnotifier.api.model;

public class AuthResult {
  public String expiresAt;
  public String token;

  public AuthResult(String token, String expiresAt) {
    this.token = token;
    this.expiresAt = expiresAt;
  }
}
