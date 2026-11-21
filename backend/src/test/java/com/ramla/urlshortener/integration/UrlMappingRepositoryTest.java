package com.ramla.urlshortener.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ramla.urlshortener.domain.UrlMapping;
import com.ramla.urlshortener.repository.UrlMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class UrlMappingRepositoryTest extends AbstractMySqlIntegrationTest {

    @Autowired
    private UrlMappingRepository repository;

    @Test
    void shortCodesAreCaseSensitive() {
        repository.saveAndFlush(new UrlMapping("abcdefg", "https://example.com/lower"));
        repository.saveAndFlush(new UrlMapping("ABCDEFG", "https://example.com/upper"));

        assertThat(repository.findOriginalUrlByShortCode("abcdefg")).contains("https://example.com/lower");
        assertThat(repository.findOriginalUrlByShortCode("ABCDEFG")).contains("https://example.com/upper");
    }

    @Test
    void duplicateShortCodeIsRejectedByDatabase() {
        repository.saveAndFlush(new UrlMapping("dupCode1", "https://example.com/one"));

        assertThatThrownBy(() -> repository.saveAndFlush(new UrlMapping("dupCode1", "https://example.com/two")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void unknownCodeReturnsEmpty() {
        assertThat(repository.findOriginalUrlByShortCode("missing1")).isEmpty();
    }
}
