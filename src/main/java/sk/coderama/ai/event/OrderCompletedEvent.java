package sk.coderama.ai.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCompletedEvent extends OrderEvent {
    private LocalDateTime completedAt;
    private String paymentReference;
}
