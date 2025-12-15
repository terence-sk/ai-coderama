package sk.coderama.ai.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.entity.Order;
import sk.coderama.ai.entity.OrderStatus;
import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.event.OrderCreatedEvent;
import sk.coderama.ai.event.OrderEvent;
import sk.coderama.ai.exception.ResourceNotFoundException;
import sk.coderama.ai.repository.OrderRepository;
import sk.coderama.ai.service.EventPublisher;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderCreatedHandler {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final Random random = new Random();

    @Bean
    public Consumer<OrderCreatedEvent> orderCreated() {
        return this::handleOrderCreated;
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());

        try {
            Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", event.getOrderId()));

            // Idempotency check
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                log.info("Order {} status updated to PROCESSING", order.getId());
            } else {
                log.warn("Order {} already in status {}, skipping", order.getId(), order.getStatus());
                return;
            }

            // Simulate payment processing (5 seconds)
            log.info("Simulating payment processing for order {} (5 seconds)...", order.getId());
            Thread.sleep(5000);

            // 50% success rate
            boolean paymentSuccess = random.nextBoolean();

            if (paymentSuccess) {
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                log.info("Payment successful for order {}, status updated to COMPLETED", order.getId());

                OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
                    .eventId(OrderEvent.generateEventId())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .total(order.getTotal())
                    .timestamp(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .paymentReference("PAY-" + UUID.randomUUID().toString())
                    .build();

                eventPublisher.publishOrderCompleted(completedEvent);
            } else {
                log.info("Payment failed for order {}, status remains PROCESSING", order.getId());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for order {}", event.getOrderId(), e);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent for order {}", event.getOrderId(), e);
        }
    }
}
