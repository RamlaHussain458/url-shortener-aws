package com.ramla.urlshortener.datastructure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Thread-safe Least Recently Used cache.
 *
 * <p>Data structure: a hash table combined with a doubly linked list (this is exactly what
 * {@link LinkedHashMap} in access-order mode is). The hash table gives O(1) lookup, the linked
 * list keeps entries ordered by recent use, so the eldest entry can be evicted in O(1).
 *
 * <p>A plain lock is used (not a read/write lock) because even a "get" reorders the list,
 * which makes every get a write operation.
 */
public final class LruCache<K, V> {

    private final LinkedHashMap<K, V> entries;
    private final ReentrantLock lock = new ReentrantLock();

    public LruCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        final int max = capacity;
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > max;
            }
        };
    }

    public Optional<V> get(K key) {
        lock.lock();
        try {
            return Optional.ofNullable(entries.get(key));
        } finally {
            lock.unlock();
        }
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            entries.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    /** Returns the existing value or creates, stores and returns a new one atomically. */
    public V computeIfAbsent(K key, Function<K, V> factory) {
        lock.lock();
        try {
            V value = entries.get(key);
            if (value == null) {
                value = factory.apply(key);
                entries.put(key, value);
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return entries.size();
        } finally {
            lock.unlock();
        }
    }
}
