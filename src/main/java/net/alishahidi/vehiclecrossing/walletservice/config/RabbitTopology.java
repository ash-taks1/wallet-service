package net.alishahidi.vehiclecrossing.walletservice.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitTopology {

    public static final String EXCHANGE = "wallet.events";
    public static final String DEAD_LETTER_EXCHANGE = "wallet.events.dlx";
    public static final String CONSUMER_QUEUE = "wallet-event-consumer.transactions";
    public static final String CONSUMER_DEAD_LETTER_QUEUE = CONSUMER_QUEUE + ".dlq";
    public static final String TRANSACTION_COMPLETED_BINDING = "transaction.completed.#";

    @Bean
    Declarables walletEventTopology() {
        TopicExchange events = ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
        FanoutExchange deadLetters = ExchangeBuilder.fanoutExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
        Queue queue = QueueBuilder.durable(CONSUMER_QUEUE).quorum().deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
        Queue deadLetterQueue = QueueBuilder.durable(CONSUMER_DEAD_LETTER_QUEUE).quorum().build();
        return new Declarables(events, deadLetters, queue, deadLetterQueue,
                BindingBuilder.bind(queue).to(events).with(TRANSACTION_COMPLETED_BINDING),
                BindingBuilder.bind(deadLetterQueue).to(deadLetters));
    }
}
