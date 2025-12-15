package sk.coderama.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.entity.Order;
import sk.coderama.ai.entity.OrderStatus;
import sk.coderama.ai.event.OrderEvent;
import sk.coderama.ai.event.OrderExpiredEvent;
import sk.coderama.ai.repository.OrderRepository;
import sk.coderama.ai.service.EventPublisher;
import sk.coderama.ai.service.OrderExpirationService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExpirationServiceImpl implements OrderExpirationService {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;

    private static final int EXPIRATION_MINUTES = 10;

    @Override
    @Transactional
    public void expireOldOrders() {
        LocalDateTime expirationThreshold = LocalDateTime.now().minusMinutes(EXPIRATION_MINUTES);

        log.debug("Checking for orders to expire (older than {})", expirationThreshold);

        List<Order> ordersToExpire = orderRepository
            .findByStatusInAndCreatedAtBefore(
                List.of(OrderStatus.PENDING, OrderStatus.PROCESSING),
                expirationThreshold
            );

        if (ordersToExpire.isEmpty()) {
            log.debug("No orders to expire");
            return;
        }

        log.info("Found {} orders to expire", ordersToExpire.size());

        for (Order order : ordersToExpire) {
            OrderStatus previousStatus = order.getStatus();
            order.setStatus(OrderStatus.EXPIRED);
            orderRepository.save(order);

            log.info("Order {} expired (previous status: {}, created at: {})",
                    order.getId(), previousStatus, order.getCreatedAt());

            OrderExpiredEvent expiredEvent = OrderExpiredEvent.builder()
                .eventId(OrderEvent.generateEventId())
                .orderId(order.getId())
                .userId(order.getUserId())
                .total(order.getTotal())
                .timestamp(LocalDateTime.now())
                .previousStatus(previousStatus)
                .expiredAt(LocalDateTime.now())
                .reason(String.format("Order not completed within %d minutes", EXPIRATION_MINUTES))
                .build();

            eventPublisher.publishOrderExpired(expiredEvent);
        }

        log.info("Expired {} orders", ordersToExpire.size());
    }
}
