package com.rakesh.shortline.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "links")
public class Link {
    @Id @Column(length = 32) private String code;
    @Version private Long version;
    @Column(nullable = false, length = 2048) private String destination;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private long totalClicks;
    private Instant lastClickedAt;

    protected Link() { }

    public Link(String code, String destination, String title, Instant createdAt) {
        this.code = code;
        this.destination = destination;
        this.title = title;
        this.createdAt = createdAt;
    }

    public void recordClick(Instant now) { totalClicks++; lastClickedAt = now; }
    public String getCode() { return code; }
    public String getDestination() { return destination; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public long getTotalClicks() { return totalClicks; }
    public Instant getLastClickedAt() { return lastClickedAt; }
}
