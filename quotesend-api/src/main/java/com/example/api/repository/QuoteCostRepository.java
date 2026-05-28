package com.example.api.repository;
import com.example.api.entity.QuoteCost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface QuoteCostRepository extends JpaRepository<QuoteCost, Long> {}
