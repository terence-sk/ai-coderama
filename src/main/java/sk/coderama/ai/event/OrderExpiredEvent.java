package sk.coderama.ai.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import sk.coderama.ai.entity.OrderStatus;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderExpiredEvent extends OrderEvent {
    private OrderStatus previousStatus;
    private LocalDateTime expiredAt;
    private String reason;
}
