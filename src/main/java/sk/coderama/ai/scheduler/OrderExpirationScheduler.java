package sk.coderama.ai.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sk.coderama.ai.service.OrderExpirationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpirationScheduler {

    private final OrderExpirationService orderExpirationService;

    // Run every 60 seconds
    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void checkExpiredOrders() {
        log.debug("Running scheduled order expiration check");

        try {
            orderExpirationService.expireOldOrders();
        } catch (Exception e) {
            log.error("Error during scheduled order expiration check", e);
        }
    }
}
