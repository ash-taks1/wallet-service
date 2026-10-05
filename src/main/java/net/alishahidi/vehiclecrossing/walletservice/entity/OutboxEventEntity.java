package net.alishahidi.vehiclecrossing.walletservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import net.alishahidi.vehiclecrossing.walletservice.entity.base.BaseEntity;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "outbox_events",
        indexes = @Index(name = "ix_outbox_events_pending", columnList = "published_at, created_at"))
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEventEntity extends BaseEntity {

    private static final int MAX_ERROR_LENGTH = 500;

    @Column(name = "aggregate_type", nullable = false, length = 64)
    String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 128)
    String eventType;

    @Column(name = "routing_key", nullable = false, length = 128)
    String routingKey;

    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    String payload;

    @Column(name = "trace_id", length = 64)
    String traceId;

    @Column(name = "published_at")
    Instant publishedAt;

    @Column(name = "attempts", nullable = false)
    int attempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    String lastError;

    public void markPublished(Instant at) {
        attempts++;
        publishedAt = at;
        lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        lastError = error == null || error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }
}
