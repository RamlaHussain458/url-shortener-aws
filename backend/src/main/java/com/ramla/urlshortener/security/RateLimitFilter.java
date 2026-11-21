package com.ramla.urlshortener.security;

import com.ramla.urlshortener.datastructure.TokenBucketRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Servlet filter (Chain of Responsibility) that throttles clients per IP address before the request
 * reaches any controller. Link creation has a much stricter budget than redirects.
 *
 * <p>Behind a reverse proxy, set FORWARD_HEADERS_STRATEGY=native so getRemoteAddr() returns the real
 * client IP instead of the proxy's IP. Only do that when a trusted proxy sets X-Forwarded-For.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_REQUESTS_BODY =
            "{\"type\":\"about:blank\",\"title\":\"Too many requests\",\"status\":429,"
                    + "\"detail\":\"Rate limit exceeded. Please slow down and try again shortly.\"}";

    private final TokenBucketRateLimiter createLimiter;
    private final TokenBucketRateLimiter redirectLimiter;

    public RateLimitFilter(TokenBucketRateLimiter createLimiter, TokenBucketRateLimiter redirectLimiter) {
        this.createLimiter = createLimiter;
        this.redirectLimiter = redirectLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        boolean isCreate = "POST".equalsIgnoreCase(request.getMethod());
        TokenBucketRateLimiter limiter = isCreate ? createLimiter : redirectLimiter;

        if (limiter.tryAcquire(request.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(limiter.retryAfterSeconds()));
        response.setContentType("application/problem+json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(TOO_MANY_REQUESTS_BODY);
    }
}
