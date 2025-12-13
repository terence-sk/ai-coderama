## Order Processing System

### Časť 2: Event-Driven Architecture + Background Processing

- Common requirements:

  - Use some messaging service like RabbitMq, Kafka (, may be Redis ?). Update docker compose file so this service can be created inside docker.
  - Add event bus into the project so the messages/events can be sent/published. As the transport system for event bus use the chosen messaging service.

- Order creation handling:

  - When the order is created the `OrderCreated` event has to be published
  - There will be handling of this event which:
    - Update order status: pending → processing
    - Simulate payment processing (5 second delay)
    - Update order status for 50% of cases to completed and publish `OrderCompleted` event
    - In another 50% of cases do not change the status

- Order expiration handling:

  - Add recursive job which will run every 60 seconds
  - The job find orders with status='processing' older than 10 minutes and update the status to 'expired'
  - Publish `OrderExpired` event

- Notifications handling

  - Create new notifications table and add upgrade script/code

  - When the `OrderCompleted` event is published

    - Send email notification (fake/mock - log to console)
    - Save notification to database (audit trail)

  - When the `OrderExpired` event is published
    - Save notification to database (audit trail)

---

### Expected Flow:

1. User creates order via `POST /api/orders`
2. Order saved to DB with status='pending'
3. `OrderCreated` event published
4. OrderProcessor handles event asynchronously:
   - Updates status to 'processing'
   - Simulates payment (5 sec delay)
   - Updates status to 'completed'
5. `OrderCompleted` event published
6. Notifier handles event:
   - Logs fake email to console
   - Saves notification to DB
7. CRON job runs every 60s:
   - Finds pending orders older than 10 minutes
   - Updates them to 'expired'
