package com.example.api.service;

import com.example.api.common.TokenType;
import com.example.api.dto.response.TokenVerificationResponse;
import com.example.api.dto.token.JwtInfo;
import com.example.api.dto.token.TokenPayload;
import com.example.api.entity.RedisToken;
import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import com.example.api.repository.RedisTokenRepository;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.*;

import static com.example.api.common.AppConstant.AUTHORITIES;
import static com.example.api.common.AppConstant.TOKEN_TYPE;

@Service @Slf4j @RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    // ⚠️ Đơn vị: MILLISECONDS (khớp với .env: JWT_ACCESS_EXPIRATION=3600000)
    @Value("${jwt.access-token-expiration:3600000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:1209600000}")
    private long refreshTokenExpiration;

    private final RedisTokenRepository redisTokenRepository;

    // ─────────────────────────────────────────────────────────────────────
    public TokenPayload generateAccessToken(String userId, Set<String> authorities) {
        Date now     = new Date();
        Date expired = new Date(now.getTime() + accessTokenExpiration);
        String id    = UUID.randomUUID().toString();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId).issueTime(now).expirationTime(expired).jwtID(id)
                .claim(TOKEN_TYPE, TokenType.ACCESS_TOKEN.name())
                .claim(AUTHORITIES, authorities)
                .build();
        return sign(claims, id, now, expired);
    }

    public TokenPayload generateRefreshToken(String userId) {
        Date now     = new Date();
        Date expired = new Date(now.getTime() + refreshTokenExpiration);
        String id    = UUID.randomUUID().toString();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(userId).issueTime(now).expirationTime(expired).jwtID(id)
                .claim(TOKEN_TYPE, TokenType.REFRESH_TOKEN.name())
                .build();
        return sign(claims, id, now, expired);
    }

    private TokenPayload sign(JWTClaimsSet claims, String id, Date issuedAt, Date expiredAt) {
        JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.HS512),
                new Payload(claims.toJSONObject()));
        try {
            jws.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            log.error("Token signing error: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
        return TokenPayload.builder()
                .jwtId(id).token(jws.serialize())
                .issueTime(issuedAt).expiredTime(expiredAt).build();
    }

    /** Verify ACCESS token */
    public TokenVerificationResponse verifyToken(String token) throws ParseException, JOSEException {
        SignedJWT jwt = SignedJWT.parse(token);
        String type   = (String) jwt.getJWTClaimsSet().getClaim(TOKEN_TYPE);

        if (!TokenType.ACCESS_TOKEN.name().equals(type)) {
            return TokenVerificationResponse.builder().isValid(false).build();
        }
        if (jwt.getJWTClaimsSet().getExpirationTime().before(new Date())) {
            return TokenVerificationResponse.builder().isValid(false).build();
        }
        if (redisTokenRepository.findById(jwt.getJWTClaimsSet().getJWTID()).isPresent()) {
            return TokenVerificationResponse.builder().isValid(false).build();
        }
        boolean valid = jwt.verify(new MACVerifier(secretKey));
        return TokenVerificationResponse.builder()
                .isValid(valid)
                .authorities(extractAuthorities(jwt.getJWTClaimsSet().getClaim(AUTHORITIES)))
                .build();
    }

    /** Verify REFRESH token, returns userId */
    public String verifyRefreshToken(String token) throws ParseException, JOSEException {
        SignedJWT jwt = SignedJWT.parse(token);
        String type   = (String) jwt.getJWTClaimsSet().getClaim(TOKEN_TYPE);

        if (!TokenType.REFRESH_TOKEN.name().equals(type))
            throw new AppException(ErrorCode.TOKEN_INVALID);
        if (jwt.getJWTClaimsSet().getExpirationTime().before(new Date()))
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        if (redisTokenRepository.findById(jwt.getJWTClaimsSet().getJWTID()).isPresent())
            throw new AppException(ErrorCode.TOKEN_INVALID);
        if (!jwt.verify(new MACVerifier(secretKey)))
            throw new AppException(ErrorCode.TOKEN_INVALID);

        return jwt.getJWTClaimsSet().getSubject();
    }

    /** Blacklist a token (logout) */
    public void blacklist(String token) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(token);
        Date expired  = jwt.getJWTClaimsSet().getExpirationTime();
        long ttl      = expired.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            redisTokenRepository.save(
                    RedisToken.builder()
                            .jwtId(jwt.getJWTClaimsSet().getJWTID())
                            .expirationDate(ttl).build());
        }
    }

    public JwtInfo parseToken(String token) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(token);
        return JwtInfo.builder()
                .jwtId(jwt.getJWTClaimsSet().getJWTID())
                .issueTime(jwt.getJWTClaimsSet().getIssueTime().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime())
                .expirationTime(jwt.getJWTClaimsSet().getExpirationTime().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }

    private List<String> extractAuthorities(Object claim) {
        if (claim instanceof List<?> list)
            return list.stream().map(String::valueOf).toList();
        return Collections.emptyList();
    }
}
