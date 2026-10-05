package net.alishahidi.vehiclecrossing.walletservice.repository;

import net.alishahidi.vehiclecrossing.walletservice.entity.OutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    @Query(value = """
            select * from outbox_events
            where published_at is null and deleted_at is null
            order by created_at, id
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventEntity> lockNextBatch(int limit);
}
