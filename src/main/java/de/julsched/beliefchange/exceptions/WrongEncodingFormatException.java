package de.julsched.beliefchange.exceptions;

public class WrongEncodingFormatException extends RuntimeException {

    public WrongEncodingFormatException(String errorMsg) {
        super(errorMsg);
    }
}
