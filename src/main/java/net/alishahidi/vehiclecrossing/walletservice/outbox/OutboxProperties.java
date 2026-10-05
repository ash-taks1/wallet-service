package net.alishahidi.vehiclecrossing.walletservice.outbox;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("wallet.outbox")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxProperties {

    int batchSize = 100;

    Duration confirmTimeout = Duration.ofSeconds(5);
}
