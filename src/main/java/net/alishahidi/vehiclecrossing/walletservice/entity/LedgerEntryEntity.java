package net.alishahidi.vehiclecrossing.walletservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import net.alishahidi.vehiclecrossing.walletservice.entity.base.BaseEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.EntryDirection;


@Entity
@Table(name = "ledger_entries")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LedgerEntryEntity extends BaseEntity {

    @Column(name = "transaction_id", nullable = false)
    UUID transactionId;

    @Column(name = "wallet_id", nullable = false)
    UUID walletId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 6)
    EntryDirection direction;

    @Column(name = "amount", nullable = false)
    long amount;

    @Column(name = "balance_after", nullable = false)
    long balanceAfter;

    public static LedgerEntryEntity debit(TransactionEntity transaction, UUID walletId, long balanceAfter) {
        return entry(transaction, walletId, EntryDirection.DEBIT, balanceAfter);
    }

    public static LedgerEntryEntity credit(TransactionEntity transaction, UUID walletId, long balanceAfter) {
        return entry(transaction, walletId, EntryDirection.CREDIT, balanceAfter);
    }

    private static LedgerEntryEntity entry(TransactionEntity transaction, UUID walletId, EntryDirection direction,
            long balanceAfter) {
        return LedgerEntryEntity.builder()
                .transactionId(transaction.getId())
                .walletId(walletId)
                .direction(direction)
                .amount(transaction.getAmount())
                .balanceAfter(balanceAfter)
                .build();
    }
}
