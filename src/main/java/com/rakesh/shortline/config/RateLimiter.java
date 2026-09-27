package com.rakesh.shortline.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;
import org.springframework.stereotype.Component;

/** Fixed-window admission control, intentionally scoped to one service instance. */
@Component
public class RateLimiter {
    private static final long WINDOW_NANOS = 60_000_000_000L;
    private final Map<String, Window> windows = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final int capacity;
    private final LongSupplier ticker;
    private static final class Window {
        final long started;
        int requests;
        Window(long started) { this.started = started; }
    }

    public RateLimiter() { this(10000, System::nanoTime); }
    public RateLimiter(int capacity, LongSupplier ticker) { this.capacity = capacity; this.ticker = ticker; }

    /** Returns zero when admitted, otherwise the Retry-After seconds. */
    public int acquire(String key, int limit) {
        lock.lock();
        try {
            long now = ticker.getAsLong();
            Window window = windows.get(key);
            if (window != null && now - window.started >= WINDOW_NANOS) {
                windows.remove(key); window = null;
            }
            if (window == null) {
                if (windows.size() >= capacity) {
                    windows.values().removeIf(old -> now - old.started >= WINDOW_NANOS);
                    if (windows.size() >= capacity) { return 60; }
                }
                window = new Window(now);
                windows.put(key, window);
            }
            if (window.requests >= limit) {
                return Math.max(1, (int) ((WINDOW_NANOS - (now - window.started) + 999_999_999L) / 1_000_000_000L));
            }
            window.requests++;
            return 0;
        } finally { lock.unlock(); }
    }
}
