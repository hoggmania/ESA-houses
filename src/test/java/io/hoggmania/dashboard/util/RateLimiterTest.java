package io.hoggmania.dashboard.util;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {

    @Test
    public void testBasicRateLimiting() {
        RateLimiter limiter = new RateLimiter(3, Duration.ofSeconds(60));
        String key = "test-key";
        
        assertTrue(limiter.tryAcquire(key));
        assertTrue(limiter.tryAcquire(key));
        assertTrue(limiter.tryAcquire(key));
        assertFalse(limiter.tryAcquire(key)); // Should be rate limited
    }

    @Test
    public void testRemainingRequests() {
        RateLimiter limiter = new RateLimiter(5, Duration.ofSeconds(60));
        String key = "test-key";
        
        assertEquals(5, limiter.remainingRequests(key));
        limiter.tryAcquire(key);
        assertEquals(4, limiter.remainingRequests(key));
        limiter.tryAcquire(key);
        assertEquals(3, limiter.remainingRequests(key));
    }

    @Test
    public void testWouldAllow() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60));
        String key = "test-key";
        
        assertTrue(limiter.wouldAllow(key));
        limiter.tryAcquire(key);
        assertTrue(limiter.wouldAllow(key));
        limiter.tryAcquire(key);
        assertFalse(limiter.wouldAllow(key));
    }

    @Test
    public void testDifferentKeys() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60));
        
        assertTrue(limiter.tryAcquire("key1"));
        assertTrue(limiter.tryAcquire("key1"));
        assertFalse(limiter.tryAcquire("key1"));
        
        // Different key should have its own limit
        assertTrue(limiter.tryAcquire("key2"));
        assertTrue(limiter.tryAcquire("key2"));
        assertFalse(limiter.tryAcquire("key2"));
    }

    @Test
    public void testClear() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60));
        String key = "test-key";
        
        assertTrue(limiter.tryAcquire(key));
        assertFalse(limiter.tryAcquire(key));
        
        limiter.clear();
        
        // After clear, should be able to acquire again
        assertTrue(limiter.tryAcquire(key));
    }

    @Test
    public void testWindowReset() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(2, Duration.ofMillis(100));
        String key = "test-key";
        
        assertTrue(limiter.tryAcquire(key));
        assertTrue(limiter.tryAcquire(key));
        assertFalse(limiter.tryAcquire(key));
        
        // Wait for window to expire
        Thread.sleep(150);
        
        // Should be able to acquire again after window reset
        assertTrue(limiter.tryAcquire(key));
    }
}

