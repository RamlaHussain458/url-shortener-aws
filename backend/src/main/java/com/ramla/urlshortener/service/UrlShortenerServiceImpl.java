package com.ramla.urlshortener.service;

import com.ramla.urlshortener.codegen.ShortCodeGenerator;
import com.ramla.urlshortener.config.AppProperties;
import com.ramla.urlshortener.domain.UrlMapping;
import com.ramla.urlshortener.exception.ShortCodeGenerationException;
import com.ramla.urlshortener.exception.ShortCodeNotFoundException;
import com.ramla.urlshortener.lookup.UrlLookup;
import com.ramla.urlshortener.repository.UrlMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerServiceImpl.class);

    private final UrlMappingRepository repository;
    private final UrlLookup urlLookup;
    private final ShortCodeGenerator codeGenerator;
    private final int maxAttempts;

    public UrlShortenerServiceImpl(UrlMappingRepository repository,
                                   UrlLookup urlLookup,
                                   ShortCodeGenerator codeGenerator,
                                   AppProperties properties) {
        this.repository = repository;
        this.urlLookup = urlLookup;
        this.codeGenerator = codeGenerator;
        this.maxAttempts = properties.shortCode().maxAttempts();
    }

    /**
     * Collision handling: instead of "check if the code exists, then insert" (which is racy when two
     * requests run at once), we just insert and let the database unique constraint decide. If the
     * code is already taken, we generate a new one and try again.
     */
    @Override
    public String create(String originalUrl) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String code = codeGenerator.generate();
            try {
                repository.saveAndFlush(new UrlMapping(code, originalUrl));
                return code;
            } catch (DataIntegrityViolationException e) {
                log.warn("Short code collision on attempt {} of {}", attempt, maxAttempts);
            }
        }
        throw new ShortCodeGenerationException(maxAttempts);
    }

    @Override
    public String resolve(String shortCode) {
        return urlLookup.findOriginalUrl(shortCode)
                .orElseThrow(() -> new ShortCodeNotFoundException(shortCode));
    }
}
