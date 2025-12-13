# Order Processing System REST API

A complete Spring Boot 4.0.0 REST API with JWT authentication, PostgreSQL database, and comprehensive testing for managing users, products, and orders.

## Table of Contents

- [Project Overview](#project-overview)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Database Setup](#database-setup)
- [Running the Application](#running-the-application)
- [Database Migrations](#database-migrations)
- [API Documentation](#api-documentation)
- [Authentication](#authentication)
- [Running Tests](#running-tests)
- [Sample API Requests](#sample-api-requests)
- [Troubleshooting](#troubleshooting)

## Project Overview

This is a RESTful API system for order processing that provides:

- User authentication and authorization using JWT tokens
- User management (CRUD operations)
- Product catalog management
- Order processing with order items
- Comprehensive error handling and validation
- Integration tests using Testcontainers
- API documentation with Swagger/OpenAPI

## Technologies Used

- **Java 21** - Programming language
- **Spring Boot 4.0.0** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data access layer
- **PostgreSQL 16** - Database
- **Flyway** - Database migrations
- **JWT (jjwt 0.12.5)** - Token-based authentication
- **Lombok** - Boilerplate code reduction
- **Springdoc OpenAPI 2.6.0** - API documentation (Swagger UI)
- **Testcontainers** - Integration testing with PostgreSQL
- **BCrypt** - Password hashing
- **Docker** - Database containerization
- **Maven** - Build and dependency management

## Prerequisites

Before running the application, ensure you have the following installed:

- **Java 21** or higher
- **Docker** and Docker Compose
- **Maven 3.6+** (or use the included Maven wrapper)

## Database Setup

The application uses PostgreSQL running in a Docker container.

### Start the Database

```bash
docker-compose up -d
```

This will start a PostgreSQL 16 container with:
- Database name: `orderdb`
- Username: `orderuser`
- Password: `orderpass`
- Port: `5432`

### Stop the Database

```bash
docker-compose down
```

### Remove Database Volume (Clean Slate)

```bash
docker-compose down -v
```

## Running the Application

### Build the Application

```bash
./mvnw clean package
```

### Run the Application

```bash
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`

**Note:** Flyway migrations run automatically on startup, creating all necessary tables and inserting seed data.

## Database Migrations

Database migrations are managed by Flyway and located in `src/main/resources/db/migration/`.

### Migration Files

1. **V1__create_users_table.sql** - Creates users table with email uniqueness
2. **V2__create_products_table.sql** - Creates products table with price/stock constraints
3. **V3__create_orders_table.sql** - Creates orders table with user relationship
4. **V4__create_order_items_table.sql** - Creates order items junction table
5. **V5__insert_seed_data.sql** - Inserts test data (5 users, 10 products, 8 orders)

### Naming Convention

Flyway migration files follow the pattern: `V{version}__{description}.sql`

### Verify Migrations

Connect to the database and check the `flyway_schema_history` table:

```bash
docker exec -it orderdb-postgres psql -U orderuser -d orderdb
```

```sql
SELECT * FROM flyway_schema_history;
```

## API Documentation

The API is documented using OpenAPI 3.0 and accessible via Swagger UI.

### Access Swagger UI

Once the application is running, open your browser and navigate to:

```
http://localhost:8080/swagger-ui.html
```

or

```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON

The OpenAPI specification is available at:

```
http://localhost:8080/v3/api-docs
```

## Authentication

The API uses JWT (JSON Web Token) for authentication.

### Register a New User

**Endpoint:** `POST /api/auth/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Login

**Endpoint:** `POST /api/auth/login`

**Request Body:**
```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "john@example.com"
}
```

### Using JWT Token in Requests

For all protected endpoints, include the JWT token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

**Example with curl:**
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://localhost:8080/api/users
```

**Token Expiration:** Tokens expire after 24 hours (86400000ms)

## Running Tests

The project includes comprehensive integration tests using Testcontainers.

### Run All Tests

```bash
./mvnw test
```

### Test Coverage

- **AuthControllerIntegrationTest** - 7 tests for authentication flows
- **UserControllerIntegrationTest** - 8 tests for user CRUD operations
- **ProductControllerIntegrationTest** - 7 tests for product management
- **OrderControllerIntegrationTest** - 8 tests for order processing
- **SecurityIntegrationTest** - 8 tests for JWT security

**Total:** 38+ integration tests

**Note:** Tests automatically start a PostgreSQL container using Testcontainers. Docker must be running.

## Sample API Requests

### 1. Register and Login

```bash
# Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "email": "alice@example.com",
    "password": "password123"
  }'

# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "password123"
  }'
```

### 2. User Management

```bash
# Get all users (requires authentication)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/users

# Get user by ID
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/users/1

# Create user
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob Johnson",
    "email": "bob@example.com",
    "password": "password123"
  }'

# Update user
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Bob Updated"
  }'

# Delete user
curl -X DELETE http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. Product Management

```bash
# Get all products
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/products

# Create product
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "High-performance laptop",
    "price": 999.99,
    "stock": 50
  }'

# Update product
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "price": 899.99,
    "stock": 45
  }'
```

### 4. Order Management

```bash
# Create order with items
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "total": 199.98,
    "status": "PENDING",
    "items": [
      {
        "productId": 1,
        "quantity": 2,
        "price": 99.99
      }
    ]
  }'

# Get order by ID
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/orders/1

# Get orders by user
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/orders/user/1

# Update order status
curl -X PUT http://localhost:8080/api/orders/1 \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "COMPLETED"
  }'
```

## Troubleshooting

### Database Connection Issues

**Problem:** Application fails to connect to PostgreSQL

**Solution:**
1. Ensure Docker is running
2. Check if PostgreSQL container is running: `docker ps`
3. Restart the container: `docker-compose restart`
4. Check logs: `docker-compose logs postgres`

### Port Already in Use

**Problem:** Port 8080 or 5432 already in use

**Solution:**
1. Stop other services using these ports
2. Or modify `application.properties` to use different ports:
   ```properties
   server.port=8081
   ```
3. Or modify `docker-compose.yml` for database:
   ```yaml
   ports:
     - "5433:5432"
   ```

### Flyway Migration Errors

**Problem:** Flyway fails to run migrations

**Solution:**
1. Drop the database and recreate: `docker-compose down -v && docker-compose up -d`
2. Check migration files for syntax errors
3. Verify `flyway_schema_history` table for conflicts

### JWT Token Expired

**Problem:** Receiving 401 Unauthorized after some time

**Solution:**
- Login again to get a new token (tokens expire after 24 hours)

### Tests Failing - Docker Not Running

**Problem:** Integration tests fail with Testcontainers errors

**Solution:**
- Ensure Docker is running before executing tests
- Testcontainers requires Docker to start PostgreSQL containers

### Build Failures

**Problem:** Maven build fails

**Solution:**
1. Clean the project: `./mvnw clean`
2. Ensure Java 21 is being used: `java -version`
3. Delete `.m2` repository cache if needed
4. Run with debug: `./mvnw clean package -X`

## API Endpoints Summary

### Public Endpoints
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### Protected Endpoints (Require JWT)

**Users:**
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

**Products:**
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID
- `POST /api/products` - Create product
- `PUT /api/products/{id}` - Update product
- `DELETE /api/products/{id}` - Delete product

**Orders:**
- `GET /api/orders` - Get all orders
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders/user/{userId}` - Get orders by user
- `POST /api/orders` - Create order
- `PUT /api/orders/{id}` - Update order
- `DELETE /api/orders/{id}` - Delete order

## HTTP Status Codes

- `200 OK` - Successful GET/PUT request
- `201 Created` - Successful POST request
- `204 No Content` - Successful DELETE request
- `400 Bad Request` - Validation error or bad input
- `401 Unauthorized` - Missing or invalid JWT token
- `404 Not Found` - Resource not found
- `500 Internal Server Error` - Server error

## Validation Rules

### User
- Name: max 100 characters, required
- Email: valid email format, max 100 characters, unique, required
- Password: min 8 characters, required

### Product
- Name: max 100 characters, required
- Price: >= 0, required
- Stock: >= 0, required

### Order
- User ID: must exist, required
- Total: >= 0, required
- Status: valid enum (PENDING, PROCESSING, COMPLETED, EXPIRED), required
- Items: at least one item required

### Order Item
- Product ID: must exist, required
- Quantity: >= 1, required
- Price: > 0, required

---

**Developed with Spring Boot 4.0.0 | PostgreSQL 16 | JWT Authentication**
