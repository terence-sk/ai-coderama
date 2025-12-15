package sk.coderama.ai.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class OrderEvent implements Serializable {
    private String eventId;
    private Long orderId;
    private Long userId;
    private BigDecimal total;
    private LocalDateTime timestamp;

    public static String generateEventId() {
        return UUID.randomUUID().toString();
    }
}
