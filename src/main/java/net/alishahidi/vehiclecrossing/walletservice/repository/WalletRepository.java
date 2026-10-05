package net.alishahidi.vehiclecrossing.walletservice.repository;

import jakarta.persistence.LockModeType;
import net.alishahidi.vehiclecrossing.walletservice.entity.WalletEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<WalletEntity, UUID> {

    List<WalletEntity> findByUserIdOrderByCreatedAt(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.id = :id")
    Optional<WalletEntity> findByIdForUpdate(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WalletEntity w where w.id in :ids order by w.id")
    List<WalletEntity> findAllByIdForUpdate(Collection<UUID> ids);


}
