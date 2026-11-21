package com.ramla.urlshortener.lookup;

import com.ramla.urlshortener.datastructure.LruCache;
import java.util.Optional;

/**
 * Decorator that adds an LRU cache in front of any {@link UrlLookup}.
 *
 * <p>Short links never change after creation, so cached entries never go stale and need no
 * invalidation. Misses are deliberately NOT cached, so random probing for unknown codes cannot
 * evict popular links from the cache.
 */
public class CachedUrlLookup implements UrlLookup {

    private final UrlLookup delegate;
    private final LruCache<String, String> cache;

    public CachedUrlLookup(UrlLookup delegate, LruCache<String, String> cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Optional<String> findOriginalUrl(String shortCode) {
        Optional<String> cached = cache.get(shortCode);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<String> loaded = delegate.findOriginalUrl(shortCode);
        loaded.ifPresent(url -> cache.put(shortCode, url));
        return loaded;
    }
}
