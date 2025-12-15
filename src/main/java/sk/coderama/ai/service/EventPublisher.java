package sk.coderama.ai.service;

import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.event.OrderCreatedEvent;
import sk.coderama.ai.event.OrderExpiredEvent;

public interface EventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
    void publishOrderCompleted(OrderCompletedEvent event);
    void publishOrderExpired(OrderExpiredEvent event);
}
