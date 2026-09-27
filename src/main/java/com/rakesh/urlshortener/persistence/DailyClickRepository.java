package com.rakesh.urlshortener.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DailyClickRepository extends JpaRepository<DailyClick, DailyClickId> {
    @Query("select d from DailyClick d where d.id.code = :code and d.id.day >= :since order by d.id.day")
    List<DailyClick> recent(String code, LocalDate since);
}
