package net.alishahidi.vehiclecrossing.walletservice.outbox;

import net.alishahidi.vehiclecrossing.walletservice.common.TraceId;
import net.alishahidi.vehiclecrossing.walletservice.config.RabbitTopology;
import net.alishahidi.vehiclecrossing.walletservice.entity.OutboxEventEntity;
import net.alishahidi.vehiclecrossing.walletservice.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(OutboxProperties.class)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxRelay {

    private static final int SCHEMA_VERSION = 1;

    OutboxEventRepository repository;
    TransactionTemplate transactionTemplate;
    RabbitTemplate rabbitTemplate;
    OutboxProperties properties;
    JsonMapper jsonMapper;

    @Scheduled(fixedDelayString = "${wallet.outbox.poll-interval-ms:100}")
    public void publishPendingEvents() {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    repository.lockNextBatch(properties.getBatchSize()).forEach(this::publish));
        } catch (RuntimeException e) {
            log.warn("Outbox relay run failed, will retry: {}", e.getMessage());
        }
    }

    private void publish(OutboxEventEntity event) {
        MDC.put(TraceId.MDC_KEY, event.getTraceId());
        try {
            CorrelationData correlation = new CorrelationData(event.getId().toString());
            rabbitTemplate.send(RabbitTopology.EXCHANGE, event.getRoutingKey(), toMessage(event), correlation);
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(properties.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (correlation.getReturned() != null) {
                failed(event, "not routed to any queue");
            } else if (!confirm.ack()) {
                failed(event, "rejected by broker: " + confirm.reason());
            } else {
                Instant now = Instant.now();
                event.markPublished(now);
                log.atInfo()
                        .setMessage("Event {} published to {}")
                        .addArgument(event.getEventType())
                        .addArgument(RabbitTopology.EXCHANGE)
                        .addKeyValue("event.action", "outbox.event.published")
                        .addKeyValue("event.id", event.getId())
                        .addKeyValue("transaction.id", event.getAggregateId())
                        .addKeyValue("outbox.lag_ms", Duration.between(event.getCreatedAt(), now).toMillis())
                        .log();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failed(event, "interrupted");
        } catch (Exception e) {
            failed(event, "no confirm: " + e.getMessage());
        } finally {
            MDC.remove(TraceId.MDC_KEY);
        }
    }

    private void failed(OutboxEventEntity event, String reason) {
        event.markFailed(reason);
        log.atWarn()
                .setMessage("Event {} not published yet: {}")
                .addArgument(event.getId())
                .addArgument(reason)
                .addKeyValue("event.action", "outbox.event.publish_failed")
                .log();
    }

    private Message toMessage(OutboxEventEntity event) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setMessageId(event.getId().toString());
        props.setType(event.getEventType());
        return new Message(jsonMapper.writeValueAsBytes(envelope(event)), props);
    }

    private ObjectNode envelope(OutboxEventEntity event) {
        ObjectNode envelope = jsonMapper.createObjectNode();
        envelope.put("eventId", event.getId().toString());
        envelope.put("eventType", event.getEventType());
        envelope.put("schemaVersion", SCHEMA_VERSION);
        envelope.put("occurredAt", event.getCreatedAt().toString());
        envelope.put("traceId", event.getTraceId());
        envelope.set("data", jsonMapper.readTree(event.getPayload()));
        return envelope;
    }
}
