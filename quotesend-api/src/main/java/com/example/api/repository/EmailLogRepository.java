package com.example.api.repository;

import com.example.api.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    @Query("SELECT e FROM EmailLog e WHERE e.quote.user.id = :userId ORDER BY e.sentAt DESC")
    List<EmailLog> findByUserId(@Param("userId") String userId);

    Optional<EmailLog> findByTrackingToken(String trackingToken);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.quote.user.id = :userId")
    long countByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.quote.user.id = :userId AND e.opened = true")
    long countOpenedByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(e) FROM EmailLog e WHERE e.quote.user.id = :userId AND e.clicked = true")
    long countClickedByUserId(@Param("userId") String userId);

    @Query("SELECT e FROM EmailLog e WHERE e.quote.user.id = :userId ORDER BY e.sentAt DESC LIMIT 5")
    List<EmailLog> findTop5ByUserId(@Param("userId") String userId);
}
