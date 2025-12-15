package sk.coderama.ai.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.event.OrderExpiredEvent;
import sk.coderama.ai.service.NotificationService;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderExpiredHandler {

    private final NotificationService notificationService;

    @Bean
    public Consumer<OrderExpiredEvent> orderExpired() {
        return this::handleOrderExpired;
    }

    @Transactional
    public void handleOrderExpired(OrderExpiredEvent event) {
        log.info("Processing OrderExpiredEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());

        try {
            notificationService.saveOrderExpiredNotification(event);
            log.info("Order expired notification processed for order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing OrderExpiredEvent for order {}", event.getOrderId(), e);
        }
    }
}
