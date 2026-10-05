package net.alishahidi.vehiclecrossing.walletservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequestDto {

    @NotBlank
    @Size(max = 200)
    String fullName;

    @NotBlank
    @Email
    @Size(max = 320)
    String email;

    @NotBlank
    @Size(min = 8, max = 128)
    String password;
}
