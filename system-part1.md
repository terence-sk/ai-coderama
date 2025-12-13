## Order Processing System

### Časť 1: Rest API part

**Features:**

- Users module:

  - User has following fields:
    id,
    name max length 100,
    email max length 100 and is unique,
    password string
  - Create CRUD REST API for this module
    - Validate input DTOs. If wrong return 400

- Authentication module:

  - Login REST API
    - Check user credentials (email, password) and if correct return JWT token

- Products module:

  - Product has following fields:
    id,
    name string max length 100,
    description string,
    price number >= 0,
    stock number >= 0,
    created_at timestamp
  - Create CRUD REST API for this module
    - Validate input DTOs. If wrong return 400

- Orders module:

  - Order has following fields:
    id,
    user_id ,
    total number >= 0,
    status enum (pending, processing, completed, expired),
    items schema
    id primary key,
    product_id,
    quantity number > 0,
    price number > 0
    created_at timestamp,
    updated_at timestamp
  - Create CRUD REST API for this module
    - Validate input DTOs. The rules are in scheme

- Additional requirements
  - Endpoints has to be protected with JWT Bearer token. Result of Login REST API.
  - Correctly handle error return states
    - 400 Bad Request
    - 401 Unauthorized
    - 404 Not Found
    - 500 Internal Server Error
  - Include OpenAPI/Swagger documentation
  - Integration tests (minimum 5 test cases)
  - Use Postgres DBS. Run Postgres in docker and initialize it with docker compose file. Include docker compose file in the GIT repository.
  - Include into the final solution DB upgrade mechanism. It has to contain some form of upgrade DB scripts or DB upgrade code.
  - Include into DBS also initial seed data. Can be part of the upgrade menchanism too.
  - In Readme.md document how to run DB upgrade tool and how to start the service.

