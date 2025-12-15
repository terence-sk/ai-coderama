package sk.coderama.ai.event.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import sk.coderama.ai.service.EventPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionalEventPublisher {

    private final EventPublisher eventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedInternalEvent event) {
        log.debug("Transaction committed, publishing OrderCreatedEvent to message broker");
        eventPublisher.publishOrderCreated(event.getOrderCreatedEvent());
    }
}
