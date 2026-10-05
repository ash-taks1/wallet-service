package net.alishahidi.vehiclecrossing.walletservice.repository;

import java.util.List;
import java.util.UUID;

import net.alishahidi.vehiclecrossing.walletservice.entity.TransactionEntity;
import net.alishahidi.vehiclecrossing.walletservice.entity.enums.TransactionStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {

    @Query("""
            select t from TransactionEntity t
            where (t.walletId = :walletId
                   or (t.counterpartyWalletId = :walletId and t.status = :incomingStatus))
              and t.id < :before
            order by t.id desc
            """)
    List<TransactionEntity> findHistory(UUID walletId, TransactionStatus incomingStatus, UUID before, Limit limit);
}
