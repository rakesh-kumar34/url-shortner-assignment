package com.rakesh.shortline.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LinkRepository extends JpaRepository<Link, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select link from Link link where link.code = :code")
    Optional<Link> findLocked(String code);
}
