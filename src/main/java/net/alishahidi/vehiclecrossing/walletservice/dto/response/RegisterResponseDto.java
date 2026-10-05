package net.alishahidi.vehiclecrossing.walletservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterResponseDto {
    UUID userId;
    String email;
    String fullName;
    UUID walletId;
    String currency;
}