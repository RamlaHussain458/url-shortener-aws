package com.ramla.urlshortener.exception;

public class ShortCodeGenerationException extends RuntimeException {

    public ShortCodeGenerationException(int attempts) {
        super("Could not generate a unique short code after " + attempts + " attempts");
    }
}
