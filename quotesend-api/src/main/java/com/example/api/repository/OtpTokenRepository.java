package com.example.api.repository;

import com.example.api.entity.OtpToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpTokenRepository extends CrudRepository<OtpToken, String> {}