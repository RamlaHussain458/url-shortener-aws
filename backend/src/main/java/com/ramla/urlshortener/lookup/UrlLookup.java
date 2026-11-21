package com.ramla.urlshortener.lookup;

import java.util.Optional;

/** Read side of the application: resolve a short code to its original URL. */
public interface UrlLookup {

    Optional<String> findOriginalUrl(String shortCode);
}
