package net.alishahidi.vehiclecrossing.walletservice.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.alishahidi.vehiclecrossing.walletservice.entity.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

    List<LedgerEntryEntity> findByWalletIdAndTransactionIdIn(UUID walletId, Collection<UUID> transactionIds);

    Optional<LedgerEntryEntity> findByTransactionIdAndWalletId(UUID transactionId, UUID walletId);
}
