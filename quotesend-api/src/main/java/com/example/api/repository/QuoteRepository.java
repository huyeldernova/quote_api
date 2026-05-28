package com.example.api.repository;

import com.example.api.entity.Quote;
import com.example.api.enums.QuoteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {

    List<Quote> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Quote> findByIdAndUserId(Long id, String userId);

    boolean existsByIdAndUserId(Long id, String userId);

    /**
     * Đếm quote của user trong năm — dùng date range thay vì YEAR()
     * để tương thích cả MySQL lẫn các DB khác.
     */
    @Query("SELECT COUNT(q) FROM Quote q " +
            "WHERE q.user.id = :userId " +
            "AND q.createdAt >= :start " +
            "AND q.createdAt < :end")
    long countByUserIdAndYearRange(@Param("userId")  String userId,
                                   @Param("start")    LocalDateTime start,
                                   @Param("end")      LocalDateTime end);

    long countByUserIdAndStatus(String userId, QuoteStatus status);

    @Query("SELECT COALESCE(SUM(q.totalAmount), 0) FROM Quote q WHERE q.user.id = :userId")
    Double sumTotalAmountByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(q) FROM Quote q " +
            "WHERE q.quoteNumber LIKE :prefix%")
    long countByQuoteNumberPrefix(@Param("prefix") String prefix);

    boolean existsByQuoteNumber(String quoteNumber);

    @Query("SELECT q FROM Quote q WHERE q.user.id = :userId " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(q.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(q.tourName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY q.createdAt DESC")
    List<Quote> searchQuotes(@Param("userId") String userId,
                             @Param("status") QuoteStatus status,
                             @Param("keyword") String keyword);


    Page<Quote> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    @Query("SELECT q FROM Quote q WHERE q.user.id = :userId " +
            "AND (:status IS NULL OR q.status = :status) " +
            "AND (:keyword IS NULL OR LOWER(q.clientName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(q.tourName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY q.createdAt DESC")
    Page<Quote> searchQuotes(@Param("userId") String userId,
                             @Param("status") QuoteStatus status,
                             @Param("keyword") String keyword,
                             Pageable pageable);


}
