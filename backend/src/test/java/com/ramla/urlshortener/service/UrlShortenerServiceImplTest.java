package com.ramla.urlshortener.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ramla.urlshortener.codegen.ShortCodeGenerator;
import com.ramla.urlshortener.config.AppProperties;
import com.ramla.urlshortener.domain.UrlMapping;
import com.ramla.urlshortener.exception.ShortCodeGenerationException;
import com.ramla.urlshortener.exception.ShortCodeNotFoundException;
import com.ramla.urlshortener.lookup.UrlLookup;
import com.ramla.urlshortener.repository.UrlMappingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceImplTest {

    @Mock
    private UrlMappingRepository repository;
    @Mock
    private UrlLookup urlLookup;
    @Mock
    private ShortCodeGenerator generator;

    private UrlShortenerServiceImpl service;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                "http://localhost:8080",
                new AppProperties.ShortCode(7, 3),
                new AppProperties.Cache(10),
                new AppProperties.RateLimit(true, 10, 1.0, 10, 1.0, 10));
        service = new UrlShortenerServiceImpl(repository, urlLookup, generator, properties);
    }

    @Test
    void returnsGeneratedCodeWhenSaveSucceeds() {
        when(generator.generate()).thenReturn("Abc1234");
        when(repository.saveAndFlush(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create("https://example.com")).isEqualTo("Abc1234");
    }

    @Test
    void retriesWithNewCodeOnCollision() {
        when(generator.generate()).thenReturn("AAAAAAA", "BBBBBBB");
        when(repository.saveAndFlush(any(UrlMapping.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.create("https://example.com")).isEqualTo("BBBBBBB");
        verify(repository, times(2)).saveAndFlush(any(UrlMapping.class));
    }

    @Test
    void givesUpAfterMaxAttempts() {
        when(generator.generate()).thenReturn("AAAAAAA");
        when(repository.saveAndFlush(any(UrlMapping.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.create("https://example.com"))
                .isInstanceOf(ShortCodeGenerationException.class);
        verify(repository, times(3)).saveAndFlush(any(UrlMapping.class));
    }

    @Test
    void resolveReturnsOriginalUrl() {
        when(urlLookup.findOriginalUrl("Abc1234")).thenReturn(Optional.of("https://example.com"));

        assertThat(service.resolve("Abc1234")).isEqualTo("https://example.com");
    }

    @Test
    void resolveThrowsWhenCodeUnknown() {
        when(urlLookup.findOriginalUrl("Nope123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("Nope123")).isInstanceOf(ShortCodeNotFoundException.class);
    }
}
