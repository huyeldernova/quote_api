package com.example.api.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import java.util.concurrent.TimeUnit;

@NoArgsConstructor @AllArgsConstructor @Builder @Getter @Setter
@RedisHash("otp")
public class OtpToken {
    @Id private String email;
    private String otp;
    @TimeToLive(unit = TimeUnit.MINUTES)
    private Long expiration; // 10 phút
}