package net.alishahidi.vehiclecrossing.walletservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtService {

    JwtProperties properties;
    SecretKey jwtSigningKey;

    public IssuedToken issue(UUID userId, String email) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .issuer(properties.getIssuer())
                .subject(userId.toString())
                .claim("email", email)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.getTtl())))
                .signWith(jwtSigningKey, Jwts.SIG.HS256)
                .compact();
        return new IssuedToken(token, properties.getTtl().toSeconds());
    }

    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(jwtSigningKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(UUID.fromString(claims.getSubject()), claims.get("email", String.class));
    }
}
