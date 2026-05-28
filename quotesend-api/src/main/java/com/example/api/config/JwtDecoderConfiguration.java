package com.example.api.config;

import com.example.api.dto.response.TokenVerificationResponse;
import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import com.example.api.service.JwtService;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Objects;

@Component @RequiredArgsConstructor @Slf4j
public class JwtDecoderConfiguration implements JwtDecoder {

    @Value("${jwt.secret-key}") private String secretKey;
    private final JwtService jwtService;
    private NimbusJwtDecoder nimbusJwtDecoder = null;

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            TokenVerificationResponse result = jwtService.verifyToken(token);
            if (!result.isValid()) throw new AppException(ErrorCode.TOKEN_INVALID);
            if (Objects.isNull(nimbusJwtDecoder)) {
                SecretKeySpec spec = new SecretKeySpec(
                        secretKey.getBytes(StandardCharsets.UTF_8), "HS512");
                nimbusJwtDecoder = NimbusJwtDecoder.withSecretKey(spec)
                        .macAlgorithm(MacAlgorithm.HS512).build();
            }
        } catch (ParseException | JOSEException e) {
            log.error("Token decode failed: {}", e.getMessage());
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
        return nimbusJwtDecoder.decode(token);
    }
}
