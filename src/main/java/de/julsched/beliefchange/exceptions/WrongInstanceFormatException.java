package de.julsched.beliefchange.exceptions;

public class WrongInstanceFormatException extends RuntimeException {

    public WrongInstanceFormatException(String errorMsg) {
        super(errorMsg);
    }
}
