package net.alishahidi.vehiclecrossing.walletservice.dto.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.EntryDirection;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionStatus;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionDto {
    UUID transactionId;
    String transactionCode;
    TransactionType type;
    TransactionStatus status;
    EntryDirection direction;
    long amount;
    Long balanceAfter;
    UUID walletId;
    UUID counterpartyWalletId;
    String description;
    String failureReason;
    String traceId;
    Instant createdAt;
}
