package de.julsched.beliefchange.exceptions;

public class WrongInputException extends RuntimeException {

    public WrongInputException (String errorMsg) {
        super(errorMsg);
    }
}
