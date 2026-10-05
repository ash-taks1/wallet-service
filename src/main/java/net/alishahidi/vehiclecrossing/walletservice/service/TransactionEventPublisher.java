package net.alishahidi.vehiclecrossing.walletservice.service;

import net.alishahidi.vehiclecrossing.walletservice.entity.TransactionEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.WalletEntity;
import net.alishahidi.vehiclecrossing.walletservice.outbox.OutboxWriter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class TransactionEventPublisher {

    private static final String AGGREGATE_TYPE = "transaction";

    OutboxWriter outbox;

    void deposited(TransactionEntity tx, WalletEntity wallet, long balanceAfter) {
        publish(tx, base(tx, wallet, balanceAfter));
    }

    void withdrawn(TransactionEntity tx, WalletEntity wallet, long balanceAfter) {
        publish(tx, base(tx, wallet, balanceAfter));
    }

    void transferred(TransactionEntity tx, WalletEntity source, long sourceBalanceAfter, WalletEntity target,
            long targetBalanceAfter) {
        Map<String, Object> data = base(tx, source, sourceBalanceAfter);
        data.put("counterpartyWalletId", target.getId());
        data.put("counterpartyUserId", target.getUserId());
        data.put("counterpartyBalanceAfter", targetBalanceAfter);
        data.put("description", tx.getDescription());
        publish(tx, data);
    }

    private void publish(TransactionEntity tx, Map<String, Object> data) {
        String type = tx.getType().name().toLowerCase(Locale.ROOT);
        outbox.append(AGGREGATE_TYPE, tx.getId(), "wallet." + type + ".completed", "transaction.completed." + type,
                data);
    }

    private static Map<String, Object> base(TransactionEntity tx, WalletEntity wallet, long balanceAfter) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("transactionId", tx.getId());
        data.put("transactionCode", tx.getTransactionCode());
        data.put("type", tx.getType());
        data.put("walletId", wallet.getId());
        data.put("userId", wallet.getUserId());
        data.put("amount", tx.getAmount());
        data.put("currency", wallet.getCurrency());
        data.put("balanceAfter", balanceAfter);
        data.put("createdAt", tx.getCreatedAt().toString());
        return data;
    }
}
