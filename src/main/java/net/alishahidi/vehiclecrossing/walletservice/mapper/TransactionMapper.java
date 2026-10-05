package net.alishahidi.vehiclecrossing.walletservice.mapper;

import java.util.UUID;

import net.alishahidi.vehiclecrossing.walletservice.dto.response.TransactionDto;
import net.alishahidi.vehiclecrossing.walletservice.entity.LedgerEntryEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.TransactionEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.EntryDirection;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper
public interface TransactionMapper {

    @Mapping(target = "transactionId", source = "transaction.id")
    @Mapping(target = "transactionCode", source = "transaction.transactionCode")
    @Mapping(target = "type", source = "transaction.type")
    @Mapping(target = "status", source = "transaction.status")
    @Mapping(target = "direction", expression = "java(direction(transaction, entry))")
    @Mapping(target = "amount", source = "transaction.amount")
    @Mapping(target = "balanceAfter", source = "entry.balanceAfter")
    @Mapping(target = "walletId", source = "walletId")
    @Mapping(target = "counterpartyWalletId", expression = "java(counterparty(transaction, walletId))")
    @Mapping(target = "description", source = "transaction.description")
    @Mapping(target = "failureReason", source = "transaction.failureReason")
    @Mapping(target = "traceId", source = "transaction.traceId")
    @Mapping(target = "createdAt", source = "transaction.createdAt")
    TransactionDto toDto(TransactionEntity transaction, LedgerEntryEntity entry, UUID walletId);

    default EntryDirection direction(TransactionEntity transaction, LedgerEntryEntity entry) {
        if (entry != null) {
            return entry.getDirection();
        }
        return transaction.getType() == TransactionType.DEPOSIT ? EntryDirection.CREDIT : EntryDirection.DEBIT;
    }

    default UUID counterparty(TransactionEntity transaction, UUID walletId) {
        return transaction.getWalletId().equals(walletId)
                ? transaction.getCounterpartyWalletId()
                : transaction.getWalletId();
    }
}
