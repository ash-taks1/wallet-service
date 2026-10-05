package net.alishahidi.vehiclecrossing.walletservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.alishahidi.vehiclecrossing.walletservice.dto.request.LoginRequestDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.request.RegisterRequestDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.RegisterResponseDto;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.TokenResponseDto;
import net.alishahidi.vehiclecrossing.walletservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "Registration and login")
@SecurityRequirements
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {

    AuthService authService;

    @Operation(summary = "Register a user", description = "Creates the user and an empty wallet.")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        return authService.register(request);
    }

    @Operation(summary = "Log in", description = "Got 15m valid JWT access token.")
    @PostMapping("/login")
    public TokenResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }
}
