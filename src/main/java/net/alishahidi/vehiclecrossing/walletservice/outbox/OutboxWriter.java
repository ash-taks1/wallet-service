package net.alishahidi.vehiclecrossing.walletservice.outbox;

import net.alishahidi.vehiclecrossing.walletservice.common.TraceId;
import net.alishahidi.vehiclecrossing.walletservice.entity.OutboxEventEntity;
import net.alishahidi.vehiclecrossing.walletservice.repository.OutboxEventRepository;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxWriter {

    OutboxEventRepository repository;
    JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID append(String aggregateType, UUID aggregateId, String eventType, String routingKey,
            Map<String, Object> data) {
        OutboxEventEntity event = repository.save(OutboxEventEntity.builder()
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .eventType(eventType)
                .routingKey(routingKey)
                .payload(jsonMapper.writeValueAsString(data))
                .traceId(TraceId.current())
                .build());
        log.atInfo()
                .setMessage("Event {} stored in outbox")
                .addArgument(eventType)
                .addKeyValue("event.action", "outbox.event.stored")
                .addKeyValue("event.id", event.getId())
                .addKeyValue("event.type", eventType)
                .addKeyValue("transaction.id", aggregateId)
                .log();
        return event.getId();
    }
}
