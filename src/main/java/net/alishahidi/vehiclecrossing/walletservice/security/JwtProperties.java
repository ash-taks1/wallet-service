package net.alishahidi.vehiclecrossing.walletservice.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("wallet.security.jwt")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtProperties {

    @NotBlank
    String issuer;

    @NotBlank
    @Size(min = 32)
    String secret;

    @NotNull
    Duration ttl;
}
