package net.alishahidi.vehiclecrossing.walletservice.repository;

import java.util.UUID;
import net.alishahidi.vehiclecrossing.walletservice.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, IdempotencyKeyEntity.Key> {

    @Modifying
    @Query(value = """
            insert into idempotency_keys (user_id, idem_key, request_hash, created_at)
            values (:userId, :key, :hash, now())
            on conflict do nothing
            """, nativeQuery = true)
    int tryClaim(UUID userId, String key, String hash);

    @Modifying
    @Query("update IdempotencyKeyEntity k set k.transactionId = :transactionId "
         + "where k.userId = :userId and k.idemKey = :key")
    int attachTransaction(UUID userId, String key, UUID transactionId);
}
