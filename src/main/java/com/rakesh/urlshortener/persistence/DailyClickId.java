package com.rakesh.urlshortener.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class DailyClickId implements Serializable {
    @Column(length = 32) private String code;
    @Column(name = "click_date") private LocalDate day;
    protected DailyClickId() { }
    public DailyClickId(String code, LocalDate day) { this.code = code; this.day = day; }
    public LocalDate getDay() { return day; }
    @Override public boolean equals(Object value) {
        return value instanceof DailyClickId other && Objects.equals(code, other.code) && Objects.equals(day, other.day);
    }
    @Override public int hashCode() { return Objects.hash(code, day); }
}
