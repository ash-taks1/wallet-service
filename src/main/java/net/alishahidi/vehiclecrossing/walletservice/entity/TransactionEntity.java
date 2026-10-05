package net.alishahidi.vehiclecrossing.walletservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import net.alishahidi.vehiclecrossing.walletservice.entity.base.BaseEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionStatus;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionType;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionEntity extends BaseEntity {

    @Column(name = "transaction_code", nullable = false, unique = true, updatable = false, length = 32)
    String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    TransactionStatus status;

    @Column(name = "wallet_id", nullable = false)
    UUID walletId;

    @Column(name = "counterparty_wallet_id")
    UUID counterpartyWalletId;

    @Column(name = "amount", nullable = false)
    long amount;

    @Column(name = "failure_reason", length = 64)
    String failureReason;

    @Column(name = "description")
    String description;

    @Column(name = "idempotency_key", length = 128)
    String idempotencyKey;

    @Column(name = "trace_id", length = 64)
    String traceId;

    @PrePersist
    void assignTransactionCode() {
        if (transactionCode == null) {
            byte[] random = new byte[6];
            ThreadLocalRandom.current().nextBytes(random);
            transactionCode = "TXN-" + LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE)
                    + "-" + HexFormat.of().withUpperCase().formatHex(random);
        }
    }
}
