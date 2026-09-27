package com.rakesh.shortline;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
class TestTime {
    @Bean @Primary MutableClock testClock() { return new MutableClock(); }

    static class MutableClock extends Clock {
        private volatile Instant now = Instant.parse("2030-01-01T12:00:00Z");
        void set(Instant value) { now = value; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }
    }
}
