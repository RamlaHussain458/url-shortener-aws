package com.ramla.urlshortener.service;

public interface UrlShortenerService {

    /** Stores the URL and returns its new short code. */
    String create(String originalUrl);

    /** Returns the original URL for a short code, or throws ShortCodeNotFoundException. */
    String resolve(String shortCode);
}
