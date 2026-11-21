package com.ramla.urlshortener.datastructure;

import java.util.function.LongSupplier;

/**
 * Per-key rate limiter using the token bucket algorithm.
 *
 * <p>Each client (key) has a bucket holding up to {@code capacity} tokens. Every request spends one
 * token, and tokens refill continuously at {@code refillPerSecond}. This allows short bursts while
 * enforcing an average rate, and each decision is O(1).
 *
 * <p>Buckets live in an {@link LruCache}, so memory stays bounded even if an attacker sends
 * requests from millions of different addresses: the least recently seen clients are forgotten.
 */
public final class TokenBucketRateLimiter {

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final double capacity;
    private final double refillPerSecond;
    private final LongSupplier nanoClock;
    private final LruCache<String, Bucket> buckets;

    public TokenBucketRateLimiter(int capacity, double refillPerSecond, int maxTrackedKeys) {
        this(capacity, refillPerSecond, maxTrackedKeys, System::nanoTime);
    }

    /** The clock is injectable so tests can control time. */
    public TokenBucketRateLimiter(int capacity, double refillPerSecond, int maxTrackedKeys, LongSupplier nanoClock) {
        if (capacity < 1 || refillPerSecond <= 0 || maxTrackedKeys < 1) {
            throw new IllegalArgumentException("capacity, refillPerSecond and maxTrackedKeys must be positive");
        }
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.nanoClock = nanoClock;
        this.buckets = new LruCache<>(maxTrackedKeys);
    }

    /** Returns true if the request is allowed, false if the client must wait. */
    public boolean tryAcquire(String key) {
        long now = nanoClock.getAsLong();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));
        return bucket.tryConsume(now);
    }

    /** Seconds a blocked client should wait before one token is available again. */
    public long retryAfterSeconds() {
        return Math.max(1L, (long) Math.ceil(1.0 / refillPerSecond));
    }

    private final class Bucket {
        private double tokens;
        private long lastRefillNanos;

        Bucket(double initialTokens, long nowNanos) {
            this.tokens = initialTokens;
            this.lastRefillNanos = nowNanos;
        }

        synchronized boolean tryConsume(long nowNanos) {
            long elapsed = Math.max(0L, nowNanos - lastRefillNanos);
            tokens = Math.min(capacity, tokens + (elapsed * refillPerSecond) / NANOS_PER_SECOND);
            lastRefillNanos = nowNanos;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
