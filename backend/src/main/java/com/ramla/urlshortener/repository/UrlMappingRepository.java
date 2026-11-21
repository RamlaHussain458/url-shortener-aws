package com.ramla.urlshortener.repository;

import com.ramla.urlshortener.domain.UrlMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /** Projection query: fetches only the URL column, not the whole entity. Parameterized, so SQL injection safe. */
    @Query("select u.originalUrl from UrlMapping u where u.shortCode = :shortCode")
    Optional<String> findOriginalUrlByShortCode(@Param("shortCode") String shortCode);
}
