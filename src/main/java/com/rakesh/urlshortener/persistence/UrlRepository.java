package com.rakesh.urlshortener.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UrlRepository extends JpaRepository<ShortUrl, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select url from ShortUrl url where url.code = :code")
    Optional<ShortUrl> findLocked(String code);
}
