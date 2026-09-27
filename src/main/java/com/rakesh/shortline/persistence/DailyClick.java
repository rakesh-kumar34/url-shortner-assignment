package com.rakesh.shortline.persistence;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name = "daily_clicks")
public class DailyClick {
    @EmbeddedId private DailyClickId id;
    private long clicks;
    protected DailyClick() { }
    public DailyClick(DailyClickId id) { this.id = id; }
    public DailyClickId getId() { return id; }
    public long getClicks() { return clicks; }
    public void increment() { clicks++; }
}
