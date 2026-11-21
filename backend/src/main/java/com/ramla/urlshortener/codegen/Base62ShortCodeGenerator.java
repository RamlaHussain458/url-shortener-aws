package com.ramla.urlshortener.codegen;

import java.security.SecureRandom;

/**
 * Generates random Base62 codes (0-9, A-Z, a-z).
 *
 * <p>With 7 characters there are 62^7 (about 3.5 trillion) possible codes. {@link SecureRandom}
 * is used instead of a sequential counter so codes cannot be guessed or enumerated by attackers.
 * {@code nextInt(bound)} is uniform, so there is no modulo bias. Collisions are still possible in
 * theory, so the caller relies on the database unique constraint and retries.
 */
public class Base62ShortCodeGenerator implements ShortCodeGenerator {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    private final SecureRandom random;
    private final int length;

    public Base62ShortCodeGenerator(SecureRandom random, int length) {
        if (length < 1) {
            throw new IllegalArgumentException("length must be at least 1");
        }
        this.random = random;
        this.length = length;
    }

    @Override
    public String generate() {
        char[] code = new char[length];
        for (int i = 0; i < length; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }
}
