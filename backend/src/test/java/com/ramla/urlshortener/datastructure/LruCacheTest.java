package com.ramla.urlshortener.datastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LruCacheTest {

    @Test
    void evictsLeastRecentlyUsedEntryWhenFull() {
        LruCache<String, Integer> cache = new LruCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.get("a");      // "a" is now the most recently used
        cache.put("c", 3);   // evicts "b"

        assertThat(cache.get("b")).isEmpty();
        assertThat(cache.get("a")).contains(1);
        assertThat(cache.get("c")).contains(3);
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void computeIfAbsentCreatesOnlyOnce() {
        LruCache<String, Integer> cache = new LruCache<>(5);
        int first = cache.computeIfAbsent("k", key -> 10);
        int second = cache.computeIfAbsent("k", key -> 99);

        assertThat(first).isEqualTo(10);
        assertThat(second).isEqualTo(10);
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThatThrownBy(() -> new LruCache<String, String>(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
