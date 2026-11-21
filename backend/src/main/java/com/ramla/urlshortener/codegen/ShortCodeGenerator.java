package com.ramla.urlshortener.codegen;

/**
 * Strategy interface for producing short codes. Swap the implementation (random, counter based,
 * hash based) without touching the rest of the application.
 */
public interface ShortCodeGenerator {

    String generate();
}
