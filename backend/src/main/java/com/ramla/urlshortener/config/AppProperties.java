package com.ramla.urlshortener.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe, validated application settings (bound from the "app" section of application.yml).
 * If a value is missing or invalid, the application fails fast at startup.
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String baseUrl,
        @Valid @NotNull ShortCode shortCode,
        @Valid @NotNull Cache cache,
        @Valid @NotNull RateLimit rateLimit) {

    public record ShortCode(@Min(4) @Max(16) int length, @Min(1) @Max(20) int maxAttempts) {
    }

    public record Cache(@Min(1) int maxEntries) {
    }

    public record RateLimit(
            boolean enabled,
            @Min(1) int createCapacity,
            @DecimalMin("0.001") double createRefillPerSecond,
            @Min(1) int redirectCapacity,
            @DecimalMin("0.001") double redirectRefillPerSecond,
            @Min(1) int maxTrackedClients) {
    }
}
