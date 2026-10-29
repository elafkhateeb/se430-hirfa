package com.hirfa.model;

public record Outcome(boolean ok, String message, double value) {
    public static Outcome ok(String message) {
        return new Outcome(true, message, 0);
    }
    public static Outcome ok(String message, double value) {
        return new Outcome(true, message, value);
    }
    public static Outcome fail(String message) {
        return new Outcome(false, message, 0);
    }
}
