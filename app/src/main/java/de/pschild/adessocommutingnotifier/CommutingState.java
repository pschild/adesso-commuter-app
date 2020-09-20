package de.pschild.adessocommutingnotifier;

public enum CommutingState {
  START("START"),
  END("END"),
  CANCELLED("CANCELLED");

  public final String label;

  private CommutingState(String label) {
    this.label = label;
  }
}
