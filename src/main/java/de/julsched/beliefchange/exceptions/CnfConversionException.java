package de.julsched.beliefchange.exceptions;

public class CnfConversionException extends RuntimeException {

    public CnfConversionException (Exception exception) {
        super("Failed to carry out cnf conversion", exception);
    }
}
