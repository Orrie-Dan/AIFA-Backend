package com.aifa.modules.iam.application;

import com.aifa.modules.iam.application.dto.AuthResponse;
import com.aifa.modules.iam.application.dto.LoginRequest;
import com.aifa.modules.iam.application.dto.RefreshRequest;
import com.aifa.modules.iam.application.dto.RegisterRequest;
import com.aifa.modules.iam.domain.AiMode;
import com.aifa.modules.iam.domain.RefreshToken;
import com.aifa.modules.iam.domain.User;
import com.aifa.modules.iam.infrastructure.RefreshTokenRepository;
import com.aifa.modules.iam.infrastructure.UserRepository;
import com.aifa.shared.audit.AuditLogger;
import com.aifa.shared.exception.BadRequestException;
import com.aifa.shared.exception.ConflictException;
import com.aifa.shared.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogger auditLogger;
    private final Clock clock;
    private final long refreshTokenExpirationDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditLogger auditLogger,
            Clock clock,
            @Value("${aifa.jwt.refresh-token-expiration-days}") long refreshTokenExpirationDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogger = auditLogger;
        this.clock = clock;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhoneHash(hashValue(request.phone().trim()));
        }
        user.setAiMode(AiMode.smart);
        userRepository.save(user);

        auditLogger.logAction(user.getId(), "REGISTER", "user", user.getEmail());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        auditLogger.logAction(user.getId(), "LOGIN", "user", user.getEmail());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String rawToken = request.refreshToken();
        if (!jwtService.isTokenValid(rawToken) || !"refresh".equals(jwtService.extractTokenType(rawToken))) {
            throw new BadRequestException("Invalid refresh token");
        }

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(hashValue(rawToken))
                .filter(token -> token.isActive(clock.instant()))
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        User user = stored.getUser();
        stored.setRevokedAt(clock.instant());
        refreshTokenRepository.save(stored);

        auditLogger.logAction(user.getId(), "REFRESH", "auth", "token rotated");
        return issueTokens(user);
    }

    @Transactional
    public void logout(UUID userId, String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository
                    .findByTokenHash(hashValue(refreshToken))
                    .filter(token -> token.getUser().getId().equals(userId))
                    .ifPresent(token -> {
                        token.setRevokedAt(clock.instant());
                        refreshTokenRepository.save(token);
                    });
        }
        auditLogger.logAction(userId, "LOGOUT", "auth", "session ended");
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        persistRefreshToken(user, refreshToken);
        return AuthResponse.of(accessToken, refreshToken, 15 * 60L, user.getId());
    }

    private void persistRefreshToken(User user, String rawRefreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hashValue(rawRefreshToken));
        token.setExpiresAt(clock.instant().plusSeconds(refreshTokenExpirationDays * 24 * 3600));
        refreshTokenRepository.save(token);
    }

    public static String hashValue(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
