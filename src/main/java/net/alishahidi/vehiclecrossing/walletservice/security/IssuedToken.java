package net.alishahidi.vehiclecrossing.walletservice.security;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IssuedToken {
    String value;
    long expiresInSeconds;
}
