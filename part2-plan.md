# Implementation Plan: Event-Driven Architecture for Order Processing System

## Overview
Add event-driven architecture using **RabbitMQ** and **Spring Cloud Stream** to implement asynchronous order processing, payment simulation, order expiration, and notifications.

## Technology Choices
- **Messaging**: RabbitMQ 3.13 with management UI
- **Event Bus**: Spring Cloud Stream (abstraction over RabbitMQ)
- **Scheduling**: Spring's @Scheduled with fixed delay
- **Testing**: H2 in-memory database + Spring Cloud Stream Test Binder

## Key Architectural Decisions

### 1. Event Publishing Strategy
**Use `@TransactionalEventListener(AFTER_COMMIT)`** to ensure events are only published after successful database transaction commit. This prevents "ghost events" for rolled-back transactions.

**Flow:**
1. OrderServiceImpl.createOrder() saves order to database
2. Publishes internal Spring ApplicationEvent within transaction
3. TransactionalEventListener waits for AFTER_COMMIT phase
4. Only after DB commit succeeds, event is published to RabbitMQ

### 2. Idempotency
- Each event contains unique `eventId` (UUID)
- Order handlers check status before processing (e.g., only process PENDING → PROCESSING once)
- Prevents duplicate processing if RabbitMQ delivers message multiple times

### 3. Payment Simulation
- Thread.sleep(5000) for 5-second delay
- Random.nextBoolean() for 50% success rate
- Successful: PROCESSING → COMPLETED + publish OrderCompletedEvent
- Failed: remains in PROCESSING (will eventually expire)

### 4. Expiration Handling
- Scheduled job runs every 60 seconds (fixed delay)
- Finds orders with status IN ('PENDING', 'PROCESSING') older than 10 minutes
- Updates status to EXPIRED
- Publishes OrderExpiredEvent

## Implementation Steps

### PHASE 1: Dependencies & Infrastructure

#### 1.1 Update pom.xml
**File:** `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/pom.xml`

Add before `</dependencies>`:
```xml
<!-- Spring Cloud Stream for Event-Driven Architecture -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-binder-rabbit</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-stream-test-binder</artifactId>
    <scope>test</scope>
</dependency>
```

Add `<dependencyManagement>` section before `<build>`:
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2024.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 1.2 Update docker-compose.yml
**File:** `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/docker-compose.yml`

Add RabbitMQ service after postgres:
```yaml
  rabbitmq:
    image: rabbitmq:3.13-management-alpine
    container_name: orderdb-rabbitmq
    environment:
      RABBITMQ_DEFAULT_USER: orderuser
      RABBITMQ_DEFAULT_PASS: orderpass
    ports:
      - "5672:5672"   # AMQP port
      - "15672:15672" # Management UI
    volumes:
      - rabbitmq_data:/var/lib/rabbitmq
    healthcheck:
      test: ["CMD", "rabbitmq-diagnostics", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

Update volumes section:
```yaml
volumes:
  postgres_data:
  rabbitmq_data:
```

#### 1.3 Update application.properties
**File:** `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/resources/application.properties`

Add at the end:
```properties
# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=orderuser
spring.rabbitmq.password=orderpass

# Spring Cloud Stream Configuration
spring.cloud.stream.bindings.orderCreated-out-0.destination=order.created
spring.cloud.stream.bindings.orderCompleted-out-0.destination=order.completed
spring.cloud.stream.bindings.orderExpired-out-0.destination=order.expired

spring.cloud.stream.bindings.orderCreated-in-0.destination=order.created
spring.cloud.stream.bindings.orderCreated-in-0.group=order-processing-service

spring.cloud.stream.bindings.orderCompleted-in-0.destination=order.completed
spring.cloud.stream.bindings.orderCompleted-in-0.group=notification-service

spring.cloud.stream.bindings.orderExpired-in-0.destination=order.expired
spring.cloud.stream.bindings.orderExpired-in-0.group=notification-service

# Scheduling Configuration
spring.task.scheduling.pool.size=2
```

#### 1.4 Update application-test.properties
**File:** `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/test/resources/application-test.properties`

Add at the end:
```properties
# Use test binder for Spring Cloud Stream (no RabbitMQ needed)
spring.cloud.stream.defaultBinder=test

# Disable scheduling in tests by default
spring.task.scheduling.enabled=false
```

---

### PHASE 2: Database Schema

#### 2.1 Create Notifications Table Migration
**File:** `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/resources/db/migration/V6__create_notifications_table.sql`

```sql
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL CHECK (event_type IN ('ORDER_COMPLETED', 'ORDER_EXPIRED')),
    message TEXT NOT NULL,
    notification_channel VARCHAR(50) NOT NULL CHECK (notification_channel IN ('EMAIL', 'SMS', 'PUSH')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_notifications_order_id ON notifications(order_id);
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_event_type ON notifications(event_type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);
```

---

### PHASE 3: Event Model

Create package: `sk.coderama.ai.event`

#### 3.1 Base Event Class
**File:** `src/main/java/sk/coderama/ai/event/OrderEvent.java`

```java
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
```

#### 3.2 OrderCreatedEvent
**File:** `src/main/java/sk/coderama/ai/event/OrderCreatedEvent.java`

```java
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
```

#### 3.3 OrderCompletedEvent
**File:** `src/main/java/sk/coderama/ai/event/OrderCompletedEvent.java`

```java
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
```

#### 3.4 OrderExpiredEvent
**File:** `src/main/java/sk/coderama/ai/event/OrderExpiredEvent.java`

```java
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
```

#### 3.5 Internal Spring Event
Create package: `sk.coderama.ai.event.internal`

**File:** `src/main/java/sk/coderama/ai/event/internal/OrderCreatedInternalEvent.java`

```java
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
```

---

### PHASE 4: Event Publishing

#### 4.1 Event Publisher Interface
**File:** `src/main/java/sk/coderama/ai/service/EventPublisher.java`

```java
package sk.coderama.ai.service;

import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.event.OrderCreatedEvent;
import sk.coderama.ai.event.OrderExpiredEvent;

public interface EventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
    void publishOrderCompleted(OrderCompletedEvent event);
    void publishOrderExpired(OrderExpiredEvent event);
}
```

#### 4.2 Event Publisher Implementation
**File:** `src/main/java/sk/coderama/ai/service/impl/EventPublisherImpl.java`

```java
package sk.coderama.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import sk.coderama.ai.event.*;
import sk.coderama.ai.service.EventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherImpl implements EventPublisher {

    private final StreamBridge streamBridge;

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        streamBridge.send("orderCreated-out-0", event);
    }

    @Override
    public void publishOrderCompleted(OrderCompletedEvent event) {
        log.info("Publishing OrderCompletedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        streamBridge.send("orderCompleted-out-0", event);
    }

    @Override
    public void publishOrderExpired(OrderExpiredEvent event) {
        log.info("Publishing OrderExpiredEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());
        streamBridge.send("orderExpired-out-0", event);
    }
}
```

#### 4.3 Transactional Event Listener
**File:** `src/main/java/sk/coderama/ai/event/internal/TransactionalEventPublisher.java`

```java
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
```

#### 4.4 Update OrderServiceImpl
**File:** `src/main/java/sk/coderama/ai/service/impl/OrderServiceImpl.java`

**Changes:**
1. Add import: `import org.springframework.context.ApplicationEventPublisher;`
2. Add import: `import sk.coderama.ai.event.*;`
3. Add import: `import sk.coderama.ai.event.internal.OrderCreatedInternalEvent;`
4. Add import: `import java.time.LocalDateTime;`
5. Add field: `private final ApplicationEventPublisher applicationEventPublisher;`
6. In `createOrder()` method, add before `return mapToResponse(savedOrder);` (line 91):

```java
        // Publish OrderCreated event (will be sent to RabbitMQ after transaction commits)
        OrderCreatedEvent orderCreatedEvent = OrderCreatedEvent.builder()
            .eventId(OrderEvent.generateEventId())
            .orderId(savedOrder.getId())
            .userId(savedOrder.getUserId())
            .total(savedOrder.getTotal())
            .status(savedOrder.getStatus())
            .timestamp(LocalDateTime.now())
            .items(savedOrder.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderItemDto.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build())
                .collect(Collectors.toList()))
            .build();

        applicationEventPublisher.publishEvent(
            new OrderCreatedInternalEvent(this, orderCreatedEvent));

        log.info("OrderCreatedEvent queued for order {} (will publish after commit)",
            savedOrder.getId());
```

---

### PHASE 5: Notification System

#### 5.1 Notification Enums

**File:** `src/main/java/sk/coderama/ai/entity/NotificationEventType.java`
```java
package sk.coderama.ai.entity;

public enum NotificationEventType {
    ORDER_COMPLETED,
    ORDER_EXPIRED
}
```

**File:** `src/main/java/sk/coderama/ai/entity/NotificationChannel.java`
```java
package sk.coderama.ai.entity;

public enum NotificationChannel {
    EMAIL,
    SMS,
    PUSH
}
```

**File:** `src/main/java/sk/coderama/ai/entity/NotificationStatus.java`
```java
package sk.coderama.ai.entity;

public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}
```

#### 5.2 Notification Entity
**File:** `src/main/java/sk/coderama/ai/entity/Notification.java`

```java
package sk.coderama.ai.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Order order;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private NotificationEventType eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", nullable = false, length = 50)
    private NotificationChannel notificationChannel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

#### 5.3 Notification Repository
**File:** `src/main/java/sk/coderama/ai/repository/NotificationRepository.java`

```java
package sk.coderama.ai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sk.coderama.ai.entity.Notification;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByOrderId(Long orderId);
    List<Notification> findByUserId(Long userId);
}
```

#### 5.4 Notification Service
**File:** `src/main/java/sk/coderama/ai/service/NotificationService.java`

```java
package sk.coderama.ai.service;

import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.event.OrderExpiredEvent;

public interface NotificationService {
    void sendOrderCompletedEmail(OrderCompletedEvent event);
    void saveOrderCompletedNotification(OrderCompletedEvent event);
    void saveOrderExpiredNotification(OrderExpiredEvent event);
}
```

**File:** `src/main/java/sk/coderama/ai/service/impl/NotificationServiceImpl.java`

```java
package sk.coderama.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.entity.*;
import sk.coderama.ai.event.*;
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
```

---

### PHASE 6: Event Handlers

Create package: `sk.coderama.ai.handler`

#### 6.1 Order Created Handler
**File:** `src/main/java/sk/coderama/ai/handler/OrderCreatedHandler.java`

**Key logic:**
- Listens for OrderCreatedEvent from RabbitMQ
- Updates order status: PENDING → PROCESSING
- Simulates payment (Thread.sleep 5 seconds)
- 50% success: update to COMPLETED + publish OrderCompletedEvent
- 50% failure: remains PROCESSING

```java
package sk.coderama.ai.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.entity.*;
import sk.coderama.ai.event.*;
import sk.coderama.ai.exception.ResourceNotFoundException;
import sk.coderama.ai.repository.OrderRepository;
import sk.coderama.ai.service.EventPublisher;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderCreatedHandler {

    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final Random random = new Random();

    @Bean
    public Consumer<OrderCreatedEvent> orderCreated() {
        return this::handleOrderCreated;
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());

        try {
            Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", event.getOrderId()));

            // Idempotency check
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(order);
                log.info("Order {} status updated to PROCESSING", order.getId());
            } else {
                log.warn("Order {} already in status {}, skipping", order.getId(), order.getStatus());
                return;
            }

            // Simulate payment processing (5 seconds)
            log.info("Simulating payment processing for order {} (5 seconds)...", order.getId());
            Thread.sleep(5000);

            // 50% success rate
            boolean paymentSuccess = random.nextBoolean();

            if (paymentSuccess) {
                order.setStatus(OrderStatus.COMPLETED);
                orderRepository.save(order);
                log.info("Payment successful for order {}, status updated to COMPLETED", order.getId());

                OrderCompletedEvent completedEvent = OrderCompletedEvent.builder()
                    .eventId(OrderEvent.generateEventId())
                    .orderId(order.getId())
                    .userId(order.getUserId())
                    .total(order.getTotal())
                    .timestamp(LocalDateTime.now())
                    .completedAt(LocalDateTime.now())
                    .paymentReference("PAY-" + UUID.randomUUID().toString())
                    .build();

                eventPublisher.publishOrderCompleted(completedEvent);
            } else {
                log.info("Payment failed for order {}, status remains PROCESSING", order.getId());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for order {}", event.getOrderId(), e);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent for order {}", event.getOrderId(), e);
        }
    }
}
```

#### 6.2 Order Completed Handler
**File:** `src/main/java/sk/coderama/ai/handler/OrderCompletedHandler.java`

```java
package sk.coderama.ai.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.event.OrderCompletedEvent;
import sk.coderama.ai.service.NotificationService;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderCompletedHandler {

    private final NotificationService notificationService;

    @Bean
    public Consumer<OrderCompletedEvent> orderCompleted() {
        return this::handleOrderCompleted;
    }

    @Transactional
    public void handleOrderCompleted(OrderCompletedEvent event) {
        log.info("Processing OrderCompletedEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());

        try {
            notificationService.sendOrderCompletedEmail(event);
            notificationService.saveOrderCompletedNotification(event);
            log.info("Order completed notification processed for order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing OrderCompletedEvent for order {}", event.getOrderId(), e);
        }
    }
}
```

#### 6.3 Order Expired Handler
**File:** `src/main/java/sk/coderama/ai/handler/OrderExpiredHandler.java`

```java
package sk.coderama.ai.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.event.OrderExpiredEvent;
import sk.coderama.ai.service.NotificationService;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderExpiredHandler {

    private final NotificationService notificationService;

    @Bean
    public Consumer<OrderExpiredEvent> orderExpired() {
        return this::handleOrderExpired;
    }

    @Transactional
    public void handleOrderExpired(OrderExpiredEvent event) {
        log.info("Processing OrderExpiredEvent for order {}, eventId: {}",
                event.getOrderId(), event.getEventId());

        try {
            notificationService.saveOrderExpiredNotification(event);
            log.info("Order expired notification processed for order {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Error processing OrderExpiredEvent for order {}", event.getOrderId(), e);
        }
    }
}
```

---

### PHASE 7: Scheduled Order Expiration

#### 7.1 Scheduling Configuration
**File:** `src/main/java/sk/coderama/ai/config/SchedulingConfig.java`

```java
package sk.coderama.ai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
    value = "spring.task.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SchedulingConfig {
}
```

#### 7.2 Update OrderRepository
**File:** `src/main/java/sk/coderama/ai/repository/OrderRepository.java`

Add method:
```java
import java.time.LocalDateTime;

List<Order> findByStatusInAndCreatedAtBefore(List<OrderStatus> statuses, LocalDateTime createdAtBefore);
```

#### 7.3 Order Expiration Service
**File:** `src/main/java/sk/coderama/ai/service/OrderExpirationService.java`

```java
package sk.coderama.ai.service;

public interface OrderExpirationService {
    void expireOldOrders();
}
```

**File:** `src/main/java/sk/coderama/ai/service/impl/OrderExpirationServiceImpl.java`

```java
package sk.coderama.ai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sk.coderama.ai.entity.*;
import sk.coderama.ai.event.*;
import sk.coderama.ai.repository.OrderRepository;
import sk.coderama.ai.service.*;
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
```

#### 7.4 Scheduler
Create package: `sk.coderama.ai.scheduler`

**File:** `src/main/java/sk/coderama/ai/scheduler/OrderExpirationScheduler.java`

```java
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
```

---

## Expected Flow

### Order Creation Flow
1. User calls `POST /api/orders` → OrderController → OrderService.createOrder()
2. Order saved to DB with status='PENDING'
3. Internal OrderCreatedInternalEvent published within transaction
4. Transaction commits
5. TransactionalEventPublisher catches AFTER_COMMIT event
6. OrderCreatedEvent published to RabbitMQ exchange 'order.created'

### Order Processing Flow
7. OrderCreatedHandler consumes event from RabbitMQ
8. Updates order status: PENDING → PROCESSING
9. Simulates payment (5 second delay)
10. 50% success: Updates to COMPLETED + publishes OrderCompletedEvent
11. 50% failure: Remains PROCESSING (will eventually expire)

### Notification Flow
12. OrderCompletedHandler consumes OrderCompletedEvent
13. Logs fake email to console
14. Saves notification to database (status=SENT)

### Expiration Flow
15. Scheduler runs every 60 seconds
16. Finds orders with status IN ('PENDING', 'PROCESSING') older than 10 minutes
17. Updates status to EXPIRED
18. Publishes OrderExpiredEvent
19. OrderExpiredHandler saves notification (status=PENDING, no email)

---

## Critical Files Summary

**Infrastructure:**
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/pom.xml`
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/docker-compose.yml`
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/resources/application.properties`
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/test/resources/application-test.properties`

**Database:**
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/resources/db/migration/V6__create_notifications_table.sql`

**Core Integration Point:**
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/java/sk/coderama/ai/service/impl/OrderServiceImpl.java` (add event publishing)
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/java/sk/coderama/ai/repository/OrderRepository.java` (add query method)

**Event Handlers (Critical Business Logic):**
- `src/main/java/sk/coderama/ai/handler/OrderCreatedHandler.java` (payment processing)
- `src/main/java/sk/coderama/ai/handler/OrderCompletedHandler.java` (notifications)
- `src/main/java/sk/coderama/ai/handler/OrderExpiredHandler.java` (expiration notifications)

---

## Verification Steps

1. **Start RabbitMQ:** `docker-compose up -d` → Access UI at http://localhost:15672
2. **Run Application:** Check logs for Spring Cloud Stream binding creation
3. **Create Order:** POST /api/orders → Watch logs for event flow
4. **Verify Payment Processing:** 5-second delay, 50% completion rate
5. **Check Notifications:** Email logged to console, DB entry created
6. **Test Expiration:** Create order, wait 10+ minutes, verify status change
7. **Verify RabbitMQ:** Check management UI for message counts on exchanges

---

## Testing Strategy

- Use H2 + Spring Cloud Stream Test Binder for integration tests
- No RabbitMQ needed for automated tests
- Scheduling disabled in tests (spring.task.scheduling.enabled=false)
- Test complete event flows end-to-end
