package net.alishahidi.vehiclecrossing.walletservice.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.alishahidi.vehiclecrossing.walletservice.dto.response.TransactionDto;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionStatus;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OperationResult {

    TransactionDto transaction;
    boolean replayed;

    public boolean succeeded() {
        return transaction.getStatus() == TransactionStatus.COMPLETED;
    }
}
