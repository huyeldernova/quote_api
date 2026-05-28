package com.example.api.service;

import com.example.api.common.AppConstant;
import com.example.api.dto.request.*;
import com.example.api.dto.response.*;
import com.example.api.dto.token.TokenPayload;
import com.example.api.entity.OtpToken;
import com.example.api.entity.Role;
import com.example.api.entity.User;
import com.example.api.exception.AppException;
import com.example.api.exception.ErrorCode;
import com.example.api.repository.OtpTokenRepository;
import com.example.api.repository.RoleRepository;
import com.example.api.repository.UserRepository;
import com.example.api.util.SecurityUtils;
import com.nimbusds.jose.JOSEException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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
    private final OtpTokenRepository otpTokenRepository;
    private final JavaMailSender mailSender;
    private final EmailService emailService;
    @Value("${spring.mail.username}")
    private String fromEmail;

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
        emailService.sendWelcomeEmail(user.getEmail(), user.getName());
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

    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        String userId = securityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
            throw new AppException(ErrorCode.PASSWORD_INCORRECT);

        // Kiểm tra confirm password
        if (!req.getNewPassword().equals(req.getConfirmPassword()))
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }


    public void forgotPassword(ForgotPasswordRequest req) {
        // Kiểm tra email tồn tại
        userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));

        // Generate OTP 6 số
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Lưu OTP vào Redis — TTL 10 phút
        otpTokenRepository.save(OtpToken.builder()
                .email(req.getEmail())
                .otp(otp)
                .expiration(10L)
                .build());

        // Gửi email OTP
        sendOtpEmail(req.getEmail(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        // Kiểm tra OTP
        OtpToken otpToken = otpTokenRepository.findById(req.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.OTP_INVALID));

        if (!otpToken.getOtp().equals(req.getOtp()))
            throw new AppException(ErrorCode.OTP_INVALID);

        // Kiểm tra confirm password
        if (!req.getNewPassword().equals(req.getConfirmPassword()))
            throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);

        // Cập nhật mật khẩu
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        // Xóa OTP khỏi Redis
        otpTokenRepository.deleteById(req.getEmail());
    }

    private void sendOtpEmail(String email, String otp) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(fromEmail, "Tourist Leader");
            helper.setTo(email);
            helper.setSubject("Password Reset OTP - Tourist Leader");
            helper.setText("""
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto;">
                    <div style="background:#0F2050;padding:20px;text-align:center;">
                        <h1 style="color:white;margin:0;">TOURIST LEADER</h1>
                        <div style="color:#C9A84C;font-size:12px;">EXPLORE THE WORLD</div>
                    </div>
                    <div style="padding:30px;background:#fff;">
                        <h2 style="color:#0F2050;">Password Reset Request</h2>
                        <p>Your OTP code is:</p>
                        <div style="background:#EEF3FB;border-left:4px solid #1E3A6E;
                                    padding:20px;text-align:center;margin:20px 0;">
                            <span style="font-size:36px;font-weight:bold;
                                         color:#1E3A6E;letter-spacing:8px;">%s</span>
                        </div>
                        <p style="color:#6B7280;">This OTP will expire in <strong>10 minutes</strong>.</p>
                        <p style="color:#6B7280;">If you did not request this, please ignore this email.</p>
                    </div>
                    <div style="background:#0F2050;padding:15px;text-align:center;
                                color:rgba(255,255,255,0.6);font-size:11px;">
                        help@touristleader.com | www.touristleader.com
                    </div>
                </div>
                """.formatted(otp), true);
            mailSender.send(mime);
        } catch (Exception e) {
            log.error("Failed to send OTP email: {}", e.getMessage());
            throw new AppException(ErrorCode.EMAIL_SEND_FAILED);
        }
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
