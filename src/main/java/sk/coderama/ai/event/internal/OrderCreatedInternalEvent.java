package sk.coderama.ai.event.internal;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import sk.coderama.ai.event.OrderCreatedEvent;

@Getter
public class OrderCreatedInternalEvent extends ApplicationEvent {
    private final OrderCreatedEvent orderCreatedEvent;

    public OrderCreatedInternalEvent(Object source, OrderCreatedEvent orderCreatedEvent) {
        super(source);
        this.orderCreatedEvent = orderCreatedEvent;
    }
}
