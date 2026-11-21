package com.ramla.urlshortener.datastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    private static final long ONE_SECOND = 1_000_000_000L;

    private final AtomicLong clock = new AtomicLong(1_000L);
    private final TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1.0, 100, clock::get);

    @Test
    void allowsBurstUpToCapacityThenBlocks() {
        assertThat(limiter.tryAcquire("client")).isTrue();
        assertThat(limiter.tryAcquire("client")).isTrue();
        assertThat(limiter.tryAcquire("client")).isTrue();
        assertThat(limiter.tryAcquire("client")).isFalse();
    }

    @Test
    void refillsOverTime() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("client");
        }
        assertThat(limiter.tryAcquire("client")).isFalse();

        clock.addAndGet(ONE_SECOND);

        assertThat(limiter.tryAcquire("client")).isTrue();
        assertThat(limiter.tryAcquire("client")).isFalse();
    }

    @Test
    void neverRefillsBeyondCapacity() {
        clock.addAndGet(100 * ONE_SECOND);

        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (limiter.tryAcquire("client")) {
                allowed++;
            }
        }
        assertThat(allowed).isEqualTo(3);
    }

    @Test
    void tracksClientsIndependently() {
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void reportsRetryAfterSeconds() {
        assertThat(limiter.retryAfterSeconds()).isEqualTo(1);
        assertThat(new TokenBucketRateLimiter(1, 0.2, 10).retryAfterSeconds()).isEqualTo(5);
    }
}
