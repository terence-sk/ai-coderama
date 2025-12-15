package sk.coderama.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.entity.Notification;
import sk.coderama.ai.entity.NotificationChannel;
import sk.coderama.ai.entity.NotificationEventType;
import sk.coderama.ai.entity.NotificationStatus;
import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.event.OrderExpiredEvent;
import sk.coderama.ai.repository.NotificationRepository;
import sk.coderama.ai.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public void sendOrderCompletedEmail(OrderCompletedEvent event) {
        // Mock email sending - log to console
        log.info("=".repeat(80));
        log.info("SENDING EMAIL NOTIFICATION");
        log.info("To: User ID {}", event.getUserId());
        log.info("Subject: Your Order #{} has been Completed!", event.getOrderId());
        log.info("Body: ");
        log.info("  Dear Customer,");
        log.info("  Your order #{} has been successfully completed!", event.getOrderId());
        log.info("  Total: ${}", event.getTotal());
        log.info("  Payment Reference: {}", event.getPaymentReference());
        log.info("  Completed at: {}", event.getCompletedAt());
        log.info("  Thank you for your order!");
        log.info("=".repeat(80));
    }

    @Override
    @Transactional
    public void saveOrderCompletedNotification(OrderCompletedEvent event) {
        String message = String.format(
            "Your order #%d has been successfully completed! Total: $%s. Payment Reference: %s",
            event.getOrderId(), event.getTotal(), event.getPaymentReference()
        );

        Notification notification = Notification.builder()
            .orderId(event.getOrderId())
            .userId(event.getUserId())
            .eventType(NotificationEventType.ORDER_COMPLETED)
            .message(message)
            .notificationChannel(NotificationChannel.EMAIL)
            .status(NotificationStatus.SENT)
            .build();

        notificationRepository.save(notification);
        log.info("Notification saved to database for order {}", event.getOrderId());
    }

    @Override
    @Transactional
    public void saveOrderExpiredNotification(OrderExpiredEvent event) {
        String message = String.format(
            "Your order #%d has expired. Previous status: %s. Reason: %s",
            event.getOrderId(), event.getPreviousStatus(), event.getReason()
        );

        Notification notification = Notification.builder()
            .orderId(event.getOrderId())
            .userId(event.getUserId())
            .eventType(NotificationEventType.ORDER_EXPIRED)
            .message(message)
            .notificationChannel(NotificationChannel.EMAIL)
            .status(NotificationStatus.PENDING)
            .build();

        notificationRepository.save(notification);
        log.info("Expiration notification saved to database for order {}", event.getOrderId());
    }
}
