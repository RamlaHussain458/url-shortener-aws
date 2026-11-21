package com.ramla.urlshortener.lookup;

import com.ramla.urlshortener.repository.UrlMappingRepository;
import java.util.Optional;

/** Looks codes up in the database. */
public class RepositoryUrlLookup implements UrlLookup {

    private final UrlMappingRepository repository;

    public RepositoryUrlLookup(UrlMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> findOriginalUrl(String shortCode) {
        return repository.findOriginalUrlByShortCode(shortCode);
    }
}
