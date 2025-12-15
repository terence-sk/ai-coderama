package sk.coderama.ai.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import sk.coderama.ai.entity.OrderStatus;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends OrderEvent {
    private OrderStatus status;
    private List<OrderItemDto> items;

    @Data
    @SuperBuilder
    @NoArgsConstructor
    public static class OrderItemDto implements java.io.Serializable {
        private Long productId;
        private Integer quantity;
        private java.math.BigDecimal price;
    }
}
