package com.mycompany.eai.camel.core.fixed;

public class FixedLengthFormatException extends RuntimeException {
    public FixedLengthFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public FixedLengthFormatException(String message) {
        super(message);
    }
}

