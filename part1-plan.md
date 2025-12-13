# Implementation Plan: Order Processing System REST API (Part 1)

## Overview
Build a complete Spring Boot 4.0.0 REST API with JWT authentication, PostgreSQL database, and comprehensive testing.

**Technology Stack (User-Approved):**
- DB Migration: Flyway
- JWT Library: jjwt (java-jwt)
- Lombok: Yes
- Password Hashing: BCrypt

## Implementation Phases

### Phase 1: Project Foundation & Dependencies ✅ COMPLETED

**Update pom.xml** with required dependencies:
- Spring Web (REST API)
- Spring Data JPA (Database access)
- Spring Security (Authentication)
- Spring Validation (Input validation)
- PostgreSQL driver
- Flyway Core + PostgreSQL adapter
- JJWT (0.12.5): api, impl, jackson
- Lombok
- Springdoc OpenAPI (2.6.0) for Swagger
- Testcontainers (PostgreSQL, JUnit Jupiter) for testing

**Critical File:** `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/pom.xml`

### Phase 2: Docker & Database Setup ✅ COMPLETED

**Create docker-compose.yml:**
- PostgreSQL 16-alpine image
- Database: orderdb, User: orderuser, Password: orderpass
- Port: 5432
- Volume for data persistence
- Health check configuration

**Update application.properties:**
- Database connection (jdbc:postgresql://localhost:5432/orderdb)
- JPA configuration (hibernate.ddl-auto=validate)
- Flyway configuration
- JWT settings (secret key, expiration: 86400000ms = 24h)
- Server configuration
- Springdoc paths

**Critical Files:**
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/docker-compose.yml`
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/resources/application.properties`

### Phase 3: Database Schema (Flyway Migrations) ✅ COMPLETED

**Create migration files in:** `src/main/resources/db/migration/`

1. **V1__create_users_table.sql**
   - Fields: id (BIGSERIAL PK), name (VARCHAR 100), email (VARCHAR 100 UNIQUE), password (VARCHAR 255), created_at, updated_at
   - Index on email

2. **V2__create_products_table.sql**
   - Fields: id, name (VARCHAR 100), description (TEXT), price (DECIMAL CHECK >= 0), stock (INTEGER CHECK >= 0), created_at, updated_at
   - Index on name

3. **V3__create_orders_table.sql**
   - Fields: id, user_id (FK to users), total (DECIMAL CHECK >= 0), status (VARCHAR 20 CHECK IN enum values), created_at, updated_at
   - Indexes on user_id, status, created_at

4. **V4__create_order_items_table.sql**
   - Fields: id, order_id (FK CASCADE), product_id (FK RESTRICT), quantity (INTEGER CHECK > 0), price (DECIMAL CHECK > 0), created_at
   - Indexes on order_id, product_id

5. **V5__insert_seed_data.sql**
   - 5 test users (BCrypt hashed passwords)
   - 10 products with realistic data
   - 8 sample orders with various statuses
   - Corresponding order items

### Phase 4: Entity Layer ✅ COMPLETED

**Create package:** `sk.coderama.ai.entity`

**Entities to create:**
1. **User.java** - @Entity, @Table(users), Lombok annotations, BCrypt password, timestamps
2. **Product.java** - @Entity, validation annotations, timestamps
3. **Order.java** - @Entity, relationship to User and OrderItem, timestamps
4. **OrderItem.java** - @Entity, relationships to Order and Product
5. **OrderStatus.java** (enum) - PENDING, PROCESSING, COMPLETED, EXPIRED

**Key patterns:**
- Use Lombok @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
- @PrePersist and @PreUpdate for timestamp management
- Proper JPA relationships (@ManyToOne, @OneToMany)

### Phase 5: Repository Layer ✅ COMPLETED

**Create package:** `sk.coderama.ai.repository`

**Repositories:**
1. **UserRepository** - extends JpaRepository, custom: existsByEmail, findByEmail
2. **ProductRepository** - extends JpaRepository
3. **OrderRepository** - extends JpaRepository, custom: findByUserId
4. **OrderItemRepository** - extends JpaRepository

### Phase 6: Security Infrastructure ✅ COMPLETED

**Create package:** `sk.coderama.ai.security`

**Core security components:**

1. **JwtTokenProvider.java**
   - generateToken(UserDetails) - creates JWT with claims
   - validateToken(String) - validates signature and expiration
   - getUserIdFromToken(String), getEmailFromToken(String)
   - Use @Value for jwt.secret and jwt.expiration

2. **JwtAuthenticationFilter.java**
   - extends OncePerRequestFilter
   - Extract Bearer token from Authorization header
   - Validate and set SecurityContext

3. **UserDetailsServiceImpl.java**
   - implements UserDetailsService
   - loadUserByUsername (using email)

4. **JwtAuthenticationEntryPoint.java**
   - implements AuthenticationEntryPoint
   - Return 401 for unauthenticated requests

**Create package:** `sk.coderama.ai.config`

5. **SecurityConfig.java**
   - @Configuration, @EnableWebSecurity
   - Configure HttpSecurity: disable CSRF, stateless sessions
   - Permit: /api/auth/**, /swagger-ui/**, /v3/api-docs/**
   - Authenticate: all other endpoints
   - Add JwtAuthenticationFilter
   - Bean: BCryptPasswordEncoder, AuthenticationManager

6. **OpenApiConfig.java**
   - Configure OpenAPI with JWT bearer authentication
   - API info, security schemes

**Critical Files:**
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/java/sk/coderama/ai/security/JwtTokenProvider.java`
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/java/sk/coderama/ai/config/SecurityConfig.java`

### Phase 7: DTOs (Data Transfer Objects) ✅ COMPLETED

**Create packages:** `sk.coderama.ai.dto.request`, `sk.coderama.ai.dto.response`

**Request DTOs (with validation):**
- LoginRequest (email, password - @NotBlank)
- RegisterRequest (name, email, password - with @Size, @Email)
- CreateUserRequest, UpdateUserRequest
- CreateProductRequest, UpdateProductRequest (price @DecimalMin, stock @Min)
- CreateOrderRequest (userId, total, status, items @NotEmpty)
- OrderItemRequest (productId, quantity @Min(1), price @DecimalMin)

**Response DTOs:**
- JwtResponse (token, type, userId, email)
- UserResponse (id, name, email, createdAt, updatedAt - NO password)
- ProductResponse
- OrderResponse (with List<OrderItemResponse>)
- OrderItemResponse
- ErrorResponse (timestamp, status, error, message, path, errors[])

### Phase 8: Exception Handling ✅ COMPLETED

**Create package:** `sk.coderama.ai.exception`

**Custom exceptions:**
- ResourceNotFoundException (404)
- BadRequestException (400)
- UnauthorizedException (401)
- DuplicateResourceException (400)

**GlobalExceptionHandler.java**
- @RestControllerAdvice
- Handle ResourceNotFoundException → 404
- Handle MethodArgumentNotValidException → 400 with field errors
- Handle DuplicateResourceException → 400
- Handle UnauthorizedException → 401
- Handle Exception → 500
- Return ErrorResponse with consistent format

**Critical File:**
- `/Users/martin/Workspace/ai_coderama/zadanie-riesenie/ai/src/main/java/sk/coderama/ai/exception/GlobalExceptionHandler.java`

### Phase 9: Service Layer ✅ COMPLETED

**Create packages:** `sk.coderama.ai.service`, `sk.coderama.ai.service.impl`

**Services to implement:**

1. **AuthService / AuthServiceImpl**
   - register(RegisterRequest) - check email uniqueness, hash password, save
   - login(LoginRequest) - validate credentials, generate JWT

2. **UserService / UserServiceImpl**
   - getAllUsers(), getUserById(id), createUser(), updateUser(), deleteUser()
   - Check email uniqueness, hash passwords, throw ResourceNotFoundException

3. **ProductService / ProductServiceImpl**
   - CRUD operations with validation

4. **OrderService / OrderServiceImpl**
   - CRUD operations
   - Create order with items in transaction
   - Calculate total from items
   - Validate products exist

**Key patterns:**
- @Service, @Transactional
- Inject repositories and PasswordEncoder
- Map entities to response DTOs
- Throw custom exceptions for error cases

### Phase 10: Controller Layer ✅ COMPLETED

**Create package:** `sk.coderama.ai.controller`

**Controllers:**

1. **AuthController** - `/api/auth`
   - POST /register (public) - RegisterRequest → UserResponse (201)
   - POST /login (public) - LoginRequest → JwtResponse (200)

2. **UserController** - `/api/users` (protected)
   - GET / - List<UserResponse>
   - GET /{id} - UserResponse
   - POST / - CreateUserRequest → UserResponse (201)
   - PUT /{id} - UpdateUserRequest → UserResponse
   - DELETE /{id} - 204 No Content

3. **ProductController** - `/api/products` (protected)
   - Same CRUD pattern as UserController

4. **OrderController** - `/api/orders` (protected)
   - Same CRUD pattern
   - Additional: GET /user/{userId} - user's orders

**Key patterns:**
- @RestController, @RequestMapping
- @Valid for validation
- @Tag, @Operation, @SecurityRequirement for Swagger
- ResponseEntity with proper HTTP status codes

### Phase 11: Integration Tests ✅ COMPLETED

**Create test base:** `src/test/java/sk/coderama/ai/BaseIntegrationTest.java`
- @SpringBootTest with RANDOM_PORT
- @Testcontainers with PostgreSQL container
- @DynamicPropertySource for test database config
- Inject TestRestTemplate and JwtTokenProvider

**Create test files:**

1. **AuthControllerIntegrationTest** (5 tests minimum)
   - Register user successfully
   - Login with valid credentials returns JWT
   - Login with invalid credentials returns 401
   - Access protected endpoint without token returns 401
   - Access protected endpoint with valid token returns 200

2. **UserControllerIntegrationTest**
   - CRUD operations with authentication
   - Validation error scenarios (400)
   - Not found scenarios (404)

3. **ProductControllerIntegrationTest**
   - CRUD operations
   - Validation (negative price, negative stock)

4. **OrderControllerIntegrationTest**
   - Create order with items
   - Get order with items
   - Validation (empty items, invalid quantity)
   - Get orders by user

5. **SecurityIntegrationTest**
   - Verify all endpoints require authentication
   - Token expiration
   - Invalid token handling

**Test configuration:** Create `src/test/resources/application-test.properties`

### Phase 12: Documentation ✅ COMPLETED

**Create README.md** with sections:
1. Project Overview
2. Technologies Used
3. Prerequisites (Java 21, Docker, Maven)
4. Database Setup
   - Start: `docker-compose up -d`
   - Stop: `docker-compose down`
5. Running the Application
   - Build: `./mvnw clean package`
   - Run: `./mvnw spring-boot:run`
   - Flyway runs automatically on startup
6. Database Migrations
   - Location and naming convention
   - How to verify (flyway_schema_history table)
7. API Documentation
   - Swagger UI: http://localhost:8080/swagger-ui.html
8. Authentication
   - How to register and login
   - Using JWT token in requests
9. Running Tests
   - `./mvnw test`
10. Sample API Requests (curl examples)
11. Troubleshooting

## Implementation Order (Step-by-Step)

1. ✅ Update pom.xml with all dependencies
2. ✅ Create docker-compose.yml
3. ✅ Update application.properties
4. ✅ Start PostgreSQL: `docker-compose up -d`
5. ✅ Create all Flyway migration scripts (V1-V5)
6. ✅ Create entity classes and enum
7. ✅ Create repository interfaces
8. ✅ Run application to execute migrations (verify DB schema)
9. ✅ Create JwtTokenProvider
10. ✅ Create JwtAuthenticationFilter
11. ✅ Create UserDetailsServiceImpl
12. ✅ Create JwtAuthenticationEntryPoint
13. ✅ Create SecurityConfig
14. ✅ Create all request DTOs with validation
15. ✅ Create all response DTOs
16. ✅ Create custom exception classes
17. ✅ Create GlobalExceptionHandler
18. ✅ Create AuthService + implementation
19. ✅ Create AuthController (test login/register manually)
20. ✅ Create UserService + implementation
21. ✅ Create UserController
22. ✅ Create ProductService + implementation
23. ✅ Create ProductController
24. ✅ Create OrderService + implementation
25. ✅ Create OrderController
26. ✅ Create OpenApiConfig
27. ✅ Test Swagger UI (add @Tag, @Operation to controllers)
28. ✅ Create BaseIntegrationTest
29. ✅ Create application-test.properties
30. ✅ Implement all 5+ integration test classes
31. ✅ Run tests: `./mvnw test`
32. ✅ Create comprehensive README.md
33. ✅ Final verification and testing

## Key Validation Rules

**User:**
- name: max 100 chars, not blank
- email: max 100 chars, valid email, unique
- password: not blank (min 8 chars recommended)

**Product:**
- name: max 100 chars, not blank
- price: >= 0, not null
- stock: >= 0, not null

**Order:**
- userId: not null, must exist
- total: >= 0, not null
- status: valid enum (PENDING, PROCESSING, COMPLETED, EXPIRED)
- items: not empty

**OrderItem:**
- productId: not null, must exist
- quantity: > 0, not null
- price: > 0, not null

## API Endpoints Summary

**Public:**
- POST /api/auth/register
- POST /api/auth/login

**Protected (JWT required):**
- /api/users - GET (list), POST, GET /{id}, PUT /{id}, DELETE /{id}
- /api/products - GET (list), POST, GET /{id}, PUT /{id}, DELETE /{id}
- /api/orders - GET (list), POST, GET /{id}, PUT /{id}, DELETE /{id}, GET /user/{userId}

**Documentation:**
- GET /swagger-ui.html
- GET /v3/api-docs

## Success Criteria

- ✅ All dependencies added to pom.xml
- ✅ PostgreSQL running in Docker
- ✅ All 4 database tables created via Flyway
- ✅ Seed data populated
- ✅ JWT authentication working (login returns token)
- ✅ All protected endpoints require Bearer token
- ✅ All CRUD endpoints functional for Users, Products, Orders
- ✅ Input validation working (400 on invalid data)
- ✅ Error handling (400, 401, 404, 500)
- ✅ Swagger UI accessible and documented
- ✅ Minimum 5 integration tests passing
- ✅ README.md with complete setup instructions
- ✅ No passwords in responses
- ✅ All tests pass: `./mvnw test`

## Critical Files to Create/Modify

**Foundation:**
- pom.xml
- docker-compose.yml
- application.properties
- application-test.properties

**Database:**
- db/migration/V1__create_users_table.sql
- db/migration/V2__create_products_table.sql
- db/migration/V3__create_orders_table.sql
- db/migration/V4__create_order_items_table.sql
- db/migration/V5__insert_seed_data.sql

**Security (Most Critical):**
- security/JwtTokenProvider.java
- security/JwtAuthenticationFilter.java
- security/UserDetailsServiceImpl.java
- security/JwtAuthenticationEntryPoint.java
- config/SecurityConfig.java

**Core Business Logic:**
- exception/GlobalExceptionHandler.java
- service/impl/AuthServiceImpl.java
- service/impl/UserServiceImpl.java
- service/impl/ProductServiceImpl.java
- service/impl/OrderServiceImpl.java

**Documentation:**
- README.md
- config/OpenApiConfig.java
