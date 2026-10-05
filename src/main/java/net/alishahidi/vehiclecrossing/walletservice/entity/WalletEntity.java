package net.alishahidi.vehiclecrossing.walletservice.entity;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import net.alishahidi.vehiclecrossing.walletservice.entity.base.BaseEntity;

@Entity
@Table(name = "wallets",
        // Last line of defence: the database itself refuses a negative balance.
        check = @CheckConstraint(name = "ck_wallets_balance_not_negative", constraint = "balance >= 0"),
        uniqueConstraints = @UniqueConstraint(name = "uq_wallets_user_currency", columnNames = {"user_id", "currency"}))
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    UUID userId;

    @Column(name = "currency", nullable = false, length = 3)
    String currency;

//    Must be BigDecimal but just for this project i use long for our test cases
    @Setter(AccessLevel.NONE)
    @Column(name = "balance", nullable = false)
    long balance;

    public boolean isOwnedBy(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public boolean hasSufficientFunds(long amount) {
        return balance >= amount;
    }

    public long credit(long amount) {
        requirePositive(amount);
        balance = Math.addExact(balance, amount);
        return balance;
    }

    public long debit(long amount) {
        requirePositive(amount);
        if (!hasSufficientFunds(amount)) {
            throw new IllegalStateException("Insufficient funds: balance=" + balance + ", amount=" + amount);
        }
        balance -= amount;
        return balance;
    }

    private static void requirePositive(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }
    }
}
