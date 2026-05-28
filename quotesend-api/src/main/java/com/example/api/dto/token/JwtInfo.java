package com.example.api.dto.token;
import lombok.*;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JwtInfo {
    private String        jwtId;
    private LocalDateTime issueTime;
    private LocalDateTime expirationTime;
}
