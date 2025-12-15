package sk.coderama.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.event.OrderCreatedEvent;
import sk.coderama.ai.event.OrderExpiredEvent;
import sk.coderama.ai.service.EventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherImpl implements EventPublisher {

    private final StreamBridge streamBridge;

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        streamBridge.send("orderCreated-out-0", event);
    }

    @Override
    public void publishOrderCompleted(OrderCompletedEvent event) {
        log.info("Publishing OrderCompletedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        streamBridge.send("orderCompleted-out-0", event);
    }

    @Override
    public void publishOrderExpired(OrderExpiredEvent event) {
        log.info("Publishing OrderExpiredEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        streamBridge.send("orderExpired-out-0", event);
    }
}
