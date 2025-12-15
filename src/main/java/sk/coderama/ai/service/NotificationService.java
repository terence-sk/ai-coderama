package sk.coderama.ai.service;

import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.event.OrderExpiredEvent;

public interface NotificationService {
    void sendOrderCompletedEmail(OrderCompletedEvent event);
    void saveOrderCompletedNotification(OrderCompletedEvent event);
    void saveOrderExpiredNotification(OrderExpiredEvent event);
}
