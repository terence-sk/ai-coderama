-- Insert test users (password is 'password123' hashed with BCrypt)
INSERT INTO users (name, email, password) VALUES
('John Doe', 'john.doe@example.com', '$2a$10$xQWL8L6pqWZ5xN3WZ3WZ3OeJZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3'),
('Jane Smith', 'jane.smith@example.com', '$2a$10$xQWL8L6pqWZ5xN3WZ3WZ3OeJZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3'),
('Bob Johnson', 'bob.johnson@example.com', '$2a$10$xQWL8L6pqWZ5xN3WZ3WZ3OeJZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3'),
('Alice Williams', 'alice.williams@example.com', '$2a$10$xQWL8L6pqWZ5xN3WZ3WZ3OeJZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3'),
('Charlie Brown', 'charlie.brown@example.com', '$2a$10$xQWL8L6pqWZ5xN3WZ3WZ3OeJZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3WZ3');

-- Insert products
INSERT INTO products (name, description, price, stock) VALUES
('Laptop', 'High-performance laptop with 16GB RAM', 1299.99, 50),
('Smartphone', 'Latest model smartphone with 5G', 899.99, 100),
('Headphones', 'Noise-cancelling wireless headphones', 249.99, 150),
('Monitor', '27-inch 4K display', 449.99, 75),
('Keyboard', 'Mechanical gaming keyboard', 129.99, 200),
('Mouse', 'Wireless ergonomic mouse', 59.99, 250),
('Tablet', '10-inch tablet with stylus', 599.99, 80),
('Webcam', '1080p HD webcam', 89.99, 120),
('USB Hub', '7-port USB 3.0 hub', 39.99, 300),
('External SSD', '1TB portable SSD', 149.99, 180);

-- Insert sample orders
INSERT INTO orders (user_id, total, status) VALUES
(1, 1549.98, 'COMPLETED'),
(1, 899.99, 'PROCESSING'),
(2, 709.97, 'COMPLETED'),
(2, 189.98, 'PENDING'),
(3, 1299.99, 'COMPLETED'),
(4, 509.98, 'PROCESSING'),
(4, 89.99, 'EXPIRED'),
(5, 679.97, 'PENDING');

-- Insert order items for order 1 (user 1)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(1, 1, 1, 1299.99),
(1, 5, 1, 129.99),
(1, 6, 2, 59.99);

-- Insert order items for order 2 (user 1)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(2, 2, 1, 899.99);

-- Insert order items for order 3 (user 2)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(3, 3, 1, 249.99),
(3, 4, 1, 449.99),
(3, 9, 1, 39.99);

-- Insert order items for order 4 (user 2)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(4, 5, 1, 129.99),
(4, 6, 1, 59.99);

-- Insert order items for order 5 (user 3)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(5, 1, 1, 1299.99);

-- Insert order items for order 6 (user 4)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(6, 4, 1, 449.99),
(6, 6, 1, 59.99);

-- Insert order items for order 7 (user 4)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(7, 8, 1, 89.99);

-- Insert order items for order 8 (user 5)
INSERT INTO order_items (order_id, product_id, quantity, price) VALUES
(8, 3, 1, 249.99),
(8, 7, 1, 599.99),
(8, 10, 2, 149.99);
