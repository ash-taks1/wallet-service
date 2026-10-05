package net.alishahidi.vehiclecrossing.walletservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenResponseDto {
    String accessToken;
    String tokenType;
    long expiresIn;
}
