package io.hoggmania.dashboard.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple token bucket rate limiter for API calls.
 * Thread-safe implementation using atomic operations.
 */
public class RateLimiter {
    
    private final int maxRequests;
    private final Duration windowDuration;
    private final ConcurrentHashMap<String, RequestWindow> windows = new ConcurrentHashMap<>();
    
    /**
     * Creates a new rate limiter.
     * 
     * @param maxRequests maximum number of requests allowed in the time window
     * @param windowDuration the duration of the time window
     */
    public RateLimiter(int maxRequests, Duration windowDuration) {
        this.maxRequests = maxRequests;
        this.windowDuration = windowDuration;
    }
    
    /**
     * Attempts to acquire a permit for the given key.
     * 
     * @param key the identifier for rate limiting (e.g., user ID, IP address)
     * @return true if the request is allowed, false if rate limit exceeded
     */
    public boolean tryAcquire(String key) {
        RequestWindow window = windows.computeIfAbsent(key, k -> new RequestWindow());
        return window.tryAcquire();
    }
    
    /**
     * Checks if a request would be allowed without consuming a permit.
     * 
     * @param key the identifier for rate limiting
     * @return true if the request would be allowed
     */
    public boolean wouldAllow(String key) {
        RequestWindow window = windows.get(key);
        if (window == null) {
            return true;
        }
        return window.wouldAllow();
    }
    
    /**
     * Gets the remaining requests for the given key.
     * 
     * @param key the identifier for rate limiting
     * @return the number of remaining requests in the current window
     */
    public int remainingRequests(String key) {
        RequestWindow window = windows.get(key);
        if (window == null) {
            return maxRequests;
        }
        return window.remainingRequests();
    }
    
    /**
     * Clears all rate limiting data.
     */
    public void clear() {
        windows.clear();
    }
    
    private class RequestWindow {
        private volatile Instant windowStart = Instant.now();
        private final AtomicInteger requestCount = new AtomicInteger(0);
        
        boolean tryAcquire() {
            resetIfExpired();
            return requestCount.incrementAndGet() <= maxRequests;
        }
        
        boolean wouldAllow() {
            resetIfExpired();
            return requestCount.get() < maxRequests;
        }
        
        int remainingRequests() {
            resetIfExpired();
            return Math.max(0, maxRequests - requestCount.get());
        }
        
        private void resetIfExpired() {
            Instant now = Instant.now();
            Instant expiry = windowStart.plus(windowDuration);
            if (now.isAfter(expiry)) {
                synchronized (this) {
                    // Double-check after acquiring lock
                    if (now.isAfter(windowStart.plus(windowDuration))) {
                        windowStart = now;
                        requestCount.set(0);
                    }
                }
            }
        }
    }
}

