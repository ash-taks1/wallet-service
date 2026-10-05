package net.alishahidi.vehiclecrossing.walletservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestFingerprintTest {

    private final UUID source = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @Test
    void sameRequestGivesSameFingerprint() {
        assertThat(RequestFingerprint.of("TRANSFER", source, target, 100L, "rent"))
                .isEqualTo(RequestFingerprint.of("TRANSFER", source, target, 100L, "rent"))
                .hasSize(64);
    }

    @Test
    void anyDifferenceGivesADifferentFingerprint() {
        String original = RequestFingerprint.of("TRANSFER", source, target, 100L, "rent");

        assertThat(RequestFingerprint.of("TRANSFER", source, target, 101L, "rent")).isNotEqualTo(original);
        assertThat(RequestFingerprint.of("TRANSFER", target, source, 100L, "rent")).isNotEqualTo(original);
        assertThat(RequestFingerprint.of("WITHDRAWAL", source, target, 100L, "rent")).isNotEqualTo(original);
    }
}
