package net.alishahidi.vehiclecrossing.walletservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys")
@IdClass(IdempotencyKeyEntity.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class IdempotencyKeyEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Id
    @Column(name = "idem_key", nullable = false, length = 128)
    String idemKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    String requestHash;

    @Column(name = "transaction_id")
    UUID transactionId;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Getter
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Key implements Serializable {
        UUID userId;
        String idemKey;
    }
}