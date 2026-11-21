package com.ramla.urlshortener.config;

import com.ramla.urlshortener.codegen.Base62ShortCodeGenerator;
import com.ramla.urlshortener.codegen.ShortCodeGenerator;
import com.ramla.urlshortener.datastructure.LruCache;
import com.ramla.urlshortener.lookup.CachedUrlLookup;
import com.ramla.urlshortener.lookup.RepositoryUrlLookup;
import com.ramla.urlshortener.lookup.UrlLookup;
import com.ramla.urlshortener.repository.UrlMappingRepository;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wires the Strategy and Decorator implementations together in one visible place. */
@Configuration
public class BeanConfig {

    @Bean
    ShortCodeGenerator shortCodeGenerator(AppProperties properties) {
        return new Base62ShortCodeGenerator(new SecureRandom(), properties.shortCode().length());
    }

    /** Database lookup wrapped by an in-memory LRU cache (Decorator pattern). */
    @Bean
    UrlLookup urlLookup(UrlMappingRepository repository, AppProperties properties) {
        return new CachedUrlLookup(
                new RepositoryUrlLookup(repository),
                new LruCache<>(properties.cache().maxEntries()));
    }
}
