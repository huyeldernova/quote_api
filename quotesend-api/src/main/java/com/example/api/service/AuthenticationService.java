package com.example.api.service;

import com.example.api.common.AppConstant;
import com.example.api.dto.request.LoginRequest;
import com.example.api.dto.request.RefreshTokenRequest;
import com.example.api.dto.request.RegisterRequest;
import com.example.api.dto.request.UpdateProfileRequest;
import com.example.api.dto.response.*;
import com.example.api.dto.token.TokenPayload;
import com.example.api.entity.Role;
import com.example.api.entity.User;
import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import com.example.api.repository.RoleRepository;
import com.example.api.repository.UserRepository;
import com.example.api.util.SecurityUtils;
import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.Set;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final PasswordEncoder       passwordEncoder;
    private final SecurityUtils         securityUtils;

    public LoginResponse login(LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = (User) auth.getPrincipal();
        return buildLoginResponse(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        Role role = roleRepository.findByName(AppConstant.USER_ROLE)
                .orElseGet(() -> roleRepository.save(
                        Role.builder().name(AppConstant.USER_ROLE).description("Agent").build()));

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .company(req.getCompany())
                .build();
        user.addRole(role);
        userRepository.save(user);
        return buildLoginResponse(user);
    }

    public RefreshTokenResponse refresh(RefreshTokenRequest req) {
        try {
            String userId = jwtService.verifyRefreshToken(req.getRefreshToken());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            Set<String> authorities = user.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
            TokenPayload access  = jwtService.generateAccessToken(userId, authorities);
            TokenPayload refresh = jwtService.generateRefreshToken(userId);
            // Blacklist the old refresh token
            jwtService.blacklist(req.getRefreshToken());
            return RefreshTokenResponse.builder()
                    .accessToken(access.getToken())
                    .refreshToken(refresh.getToken())
                    .build();
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.TOKEN_INVALID);
        }
    }

    public UserResponse getMe() {
        String userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateMe(UpdateProfileRequest req) {
        String userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (req.getName()    != null) user.setName(req.getName());
        if (req.getCompany() != null) user.setCompany(req.getCompany());
        userRepository.save(user);
        return toUserResponse(user);
    }

    public void logout(String token) {
        try { jwtService.blacklist(token); }
        catch (ParseException e) { log.warn("Could not blacklist token: {}", e.getMessage()); }
    }

    // ── helpers ───────────────────────────────────────────────────────────
    private LoginResponse buildLoginResponse(User user) {
        Set<String> authorities = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        TokenPayload access  = jwtService.generateAccessToken(user.getId(), authorities);
        TokenPayload refresh = jwtService.generateRefreshToken(user.getId());
        return LoginResponse.builder()
                .accessToken(access.getToken())
                .refreshToken(refresh.getToken())
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {
        String role = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).findFirst().orElse("USER");
        return UserResponse.builder()
                .id(user.getId()).name(user.getName())
                .email(user.getEmail()).company(user.getCompany())
                .role(role).build();
    }
}
