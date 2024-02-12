package de.julsched.beliefchange.exceptions;

public class MinDistanceException extends RuntimeException {

    public MinDistanceException (String info) {
        super("Failed to determine minimum distance between models: " + info);
    }

    public MinDistanceException (Exception exception) {
        super("Failed to determine minimum distance between models", exception);
    }
}
