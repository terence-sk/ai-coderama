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
