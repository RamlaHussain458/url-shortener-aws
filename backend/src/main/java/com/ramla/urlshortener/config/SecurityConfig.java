package com.ramla.urlshortener.config;

import com.ramla.urlshortener.datastructure.TokenBucketRateLimiter;
import com.ramla.urlshortener.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Security model: this is a public, stateless API. There are no logins and no sessions, so
 * CSRF protection (which defends session cookies) does not apply. Everything that is not
 * explicitly allowed is denied.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties properties) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/urls").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()
                        .requestMatchers(HttpMethod.GET, "/*").permitAll() // short code redirects
                        .anyRequest().denyAll())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000)));

        if (properties.rateLimit().enabled()) {
            http.addFilterBefore(buildRateLimitFilter(properties.rateLimit()), AuthorizationFilter.class);
        }
        return http.build();
    }

    private static RateLimitFilter buildRateLimitFilter(AppProperties.RateLimit config) {
        return new RateLimitFilter(
                new TokenBucketRateLimiter(config.createCapacity(), config.createRefillPerSecond(), config.maxTrackedClients()),
                new TokenBucketRateLimiter(config.redirectCapacity(), config.redirectRefillPerSecond(), config.maxTrackedClients()));
    }
}
