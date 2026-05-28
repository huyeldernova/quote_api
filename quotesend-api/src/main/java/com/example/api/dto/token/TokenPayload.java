package com.example.api.dto.token;
import lombok.*;
import java.util.Date;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenPayload {
    private String jwtId;
    private String token;
    private Date   issueTime;
    private Date   expiredTime;
}
