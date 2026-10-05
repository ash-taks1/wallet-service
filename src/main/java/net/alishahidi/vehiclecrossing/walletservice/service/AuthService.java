package net.alishahidi.vehiclecrossing.walletservice.service;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import net.alishahidi.vehiclecrossing.walletservice.dto.request.LoginRequestDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.request.RegisterRequestDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.RegisterResponseDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.TokenResponseDto;
import net.alishahidi.vehiclecrossing.walletservice.entity.UserEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.WalletEntity;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ApiException;
import net.alishahidi.vehiclecrossing.walletservice.exeption.ErrorCode;
import net.alishahidi.vehiclecrossing.walletservice.mapper.UserMapper;
import net.alishahidi.vehiclecrossing.walletservice.repository.UserRepository;
import net.alishahidi.vehiclecrossing.walletservice.repository.WalletRepository;
import net.alishahidi.vehiclecrossing.walletservice.security.IssuedToken;
import net.alishahidi.vehiclecrossing.walletservice.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthService {

    final UserRepository users;
    final WalletRepository wallets;
    final PasswordEncoder passwordEncoder;
    final JwtService jwtService;
    final UserMapper userMapper;

    @Value("${wallet.currency}")
    String defaultCurrency;

    @NonFinal
    String dummyPasswordHash;

    @PostConstruct
    void initDummyPasswordHash() {
        dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public RegisterResponseDto register(RegisterRequestDto request) {
        String email = normalize(request.getEmail());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        UserEntity user;
        WalletEntity wallet;
        try {
            user = users.saveAndFlush(UserEntity.builder()
                    .email(email)
                    .fullName(request.getFullName().trim())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .build());
            wallet = wallets.saveAndFlush(WalletEntity.builder()
                    .userId(user.getId())
                    .currency(defaultCurrency)
                    .balance(0)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        log.atInfo()
                .setMessage("User registered")
                .addKeyValue("event.action", "user.registered")
                .addKeyValue("user.id", user.getId())
                .addKeyValue("wallet.id", wallet.getId())
                .log();
        return userMapper.toRegisterResponse(user, wallet);
    }

    @Transactional(readOnly = true)
    public TokenResponseDto login(LoginRequestDto request) {
        Optional<UserEntity> user = users.findByEmail(normalize(request.getEmail()));
        String hash = user.map(UserEntity::getPasswordHash).orElse(dummyPasswordHash);
        boolean matches = passwordEncoder.matches(request.getPassword(), hash);
        if (user.isEmpty() || !matches) {
            log.atWarn()
                    .setMessage("Login failed")
                    .addKeyValue("event.action", "user.login.failed")
                    .log();
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }
        log.atInfo()
                .setMessage("User logged in")
                .addKeyValue("event.action", "user.login.succeeded")
                .addKeyValue("user.id", user.get().getId())
                .log();
        IssuedToken token = jwtService.issue(user.get().getId(), user.get().getEmail());
        return TokenResponseDto.builder()
                .accessToken(token.getValue())
                .tokenType("Bearer")
                .expiresIn(token.getExpiresInSeconds())
                .build();
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
