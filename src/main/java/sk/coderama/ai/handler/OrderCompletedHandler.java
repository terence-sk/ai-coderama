package sk.coderama.ai.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.service.NotificationService;

import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderCompletedHandler {

    private final NotificationService notificationService;

    @Bean
    public Consumer<OrderCompletedEvent> orderCompleted() {
        return this::handleOrderCompleted;
    }

    @Transactional
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Processing OrderCompletedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());

        try {
            notificationService.sendOrderCompletedEmail(event);
            notificationService.saveOrderCompletedNotification(event);
            log.info("Order completed notification processed for order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing OrderCompletedEvent for order {}", event.getOrderId(), e);
        }
    }
}
