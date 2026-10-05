package net.alishahidi.vehiclecrossing.walletservice.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class WalletEntityTest {

    private final UUID owner = UUID.randomUUID();

    @Test
    void creditAndDebitReturnTheNewBalance() {
        WalletEntity wallet = walletWith(0);

        assertThat(wallet.credit(100_000)).isEqualTo(100_000);
        assertThat(wallet.debit(3_000)).isEqualTo(97_000);
        assertThat(wallet.getBalance()).isEqualTo(97_000);
    }

    @Test
    void wholeBalanceCanBeWithdrawn() {
        WalletEntity wallet = walletWith(1_000);

        assertThat(wallet.debit(1_000)).isZero();
    }

    @Test
    void debitNeverMakesTheBalanceNegative() {
        WalletEntity wallet = walletWith(1_000);

        assertThat(wallet.hasSufficientFunds(3_000)).isFalse();
        assertThatThrownBy(() -> wallet.debit(3_000)).isInstanceOf(IllegalStateException.class);
        assertThat(wallet.getBalance()).isEqualTo(1_000);
    }

    @Test
    void zeroOrNegativeAmountsAreRejected() {
        WalletEntity wallet = walletWith(1_000);

        assertThatThrownBy(() -> wallet.credit(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> wallet.debit(-5)).isInstanceOf(IllegalArgumentException.class);
        assertThat(wallet.getBalance()).isEqualTo(1_000);
    }

    @Test
    void ownershipIsCheckedByUserId() {
        WalletEntity wallet = walletWith(0);

        assertThat(wallet.isOwnedBy(owner)).isTrue();
        assertThat(wallet.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    private WalletEntity walletWith(long balance) {
        WalletEntity wallet = WalletEntity.builder().userId(owner).currency("IRR").balance(0).build();
        if (balance > 0) {
            wallet.credit(balance);
        }
        return wallet;
    }
}
