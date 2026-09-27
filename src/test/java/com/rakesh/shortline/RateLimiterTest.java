package com.rakesh.shortline;

import com.rakesh.shortline.config.RateLimiter;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {
    @Test void windowResetUsesMonotonicTimeAndBoundsClientState() {
        var now = new AtomicLong();
        var limiter = new RateLimiter(2, now::get);
        assertThat(limiter.acquire("client-a", 2)).isZero();
        assertThat(limiter.acquire("client-a", 2)).isZero();
        assertThat(limiter.acquire("client-a", 2)).isEqualTo(60);
        assertThat(limiter.acquire("client-b", 2)).isZero();
        assertThat(limiter.acquire("client-c", 2)).isEqualTo(60);
        now.set(60_000_000_000L);
        assertThat(limiter.acquire("client-c", 2)).isZero();
        assertThat(limiter.acquire("client-a", 2)).isZero();
    }
}
