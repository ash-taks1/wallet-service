package net.alishahidi.vehiclecrossing.walletservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import net.alishahidi.vehiclecrossing.walletservice.entity.base.BaseEntity;

@Entity
@Table(name = "wallets")
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
