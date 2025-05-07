# Microservice Showcase: User Management and Journaling

This project demonstrates a microservices architecture with two Spring Boot applications:
1.  **User Service**: Manages user CRUD, roles, and authentication. Publishes user-related events to Kafka.
2.  **Journal Service**: Consumes user events from Kafka, logs them, and persists them to a database. Exposes an API to retrieve journaled events.

## System Architecture

*   **User Service** (Port 8081) -> PostgreSQL (user_db, Port 5432)
*   **User Service** -> Kafka (Topic: `user-events`, Port 9092)
*   **Journal Service** (Port 8082) <- Kafka (Topic: `user-events`)
*   **Journal Service** -> PostgreSQL (journal_db, Port 5433)

## Prerequisites

*   Java 17+
*   Maven 3.6+
*   Docker & Docker Compose

## How to Run

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd microservice-showcase
    ```

2.  **Build the services:**
    Navigate to the root of the `microservice-showcase` project (where the parent `pom.xml` is).
    ```bash
    mvn clean package -DskipTests
    ```
    This will build both `user-service.jar` and `journal-service.jar` in their respective `target` directories.

3.  **Start the system with Docker Compose:**
    From the root directory (`microservice-showcase`):
    ```bash
    docker-compose up --build
    ```
    This will:
    *   Build Docker images for `user-service` and `journal-service`.
    *   Start Zookeeper, Kafka, two PostgreSQL instances, and both microservices.
    *   Kafka topic `user-events` will be auto-created.
    *   User service will initialize `ROLE_USER` and `ROLE_ADMIN` if `data.sql` is configured and `SPRING_SQL_INIT_MODE=always`.

4.  **Wait for services to start.** Check logs:
    ```bash
    docker-compose logs -f user-service
    docker-compose logs -f journal-service
    ```
    Look for "Started UserManagementApplication" and "Started JournalServiceApplication".

## Interacting with the System

Use a tool like Postman or `curl`.

### 1. User Service (http://localhost:8081)

**Authentication:**
Most User Service endpoints (except `/api/auth/register` and `/api/auth/login`) and all Journal Service endpoints require a JWT Bearer token in the `Authorization` header.

*   **Register a new user:**
    `POST /api/auth/register`
    Body (JSON):
    ```json
    {
        "username": "testuser",
        "email": "test@example.com",
        "password": "password123"
    }
    ```
    Response: User details. This user will have `ROLE_USER`.

*   **Login to get JWT:**
    `POST /api/auth/login`
    Body (JSON):
    ```json
    {
        "username": "testuser",
        "password": "password123"
    }
    ```
    Response (JSON):
    ```json
    {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "username": "testuser",
        "authorities": [{"authority": "ROLE_USER"}]
    }
    ```
    **Copy the `token` value.** You'll need it for authenticated requests. Set it as `Bearer <token>` in the Authorization header.

*   **To create an ADMIN user:**
    There's no direct endpoint for this initially. You'd typically:
    1. Register a user.
    2. Manually update their role in the `user_db`'s `user_roles` and `roles` tables (or write a data seeding script/admin endpoint if this were a real app).
       Alternatively, modify the `UserService::registerUser` to assign `ROLE_ADMIN` for the first registered user or based on some condition for testing.
       For this demo, after registering `testuser`, you can login to `postgres-user` DB and:
    ```sql
    -- Find role_id for ROLE_ADMIN (assuming it was created by data.sql)
    SELECT id FROM roles WHERE name = 'ROLE_ADMIN'; -- let's say it's 2
    -- Find user_id for 'testuser'
    SELECT id FROM users WHERE username = 'testuser'; -- let's say it's 1
    -- Assign ROLE_ADMIN to testuser
    INSERT INTO user_roles (user_id, role_id) VALUES (1, 2);
    ```
    Then log in as `testuser` again to get a token with `ROLE_ADMIN`.

**User Management (Requires JWT with appropriate roles):**

*   **Get User Details:**
    `GET /api/users/{id}` (Requires `ROLE_USER` or `ROLE_ADMIN`)
    Example: `GET http://localhost:8081/api/users/1`

*   **Update User Information:** (Requires `ROLE_ADMIN` or being the user themselves - current setup is simpler: `ROLE_ADMIN`)
    `PUT /api/users/{id}`
    Body (JSON - only include fields to update):
    ```json
    {
        "email": "newemail@example.com"
    }
    ```

*   **Delete User:** (Requires `ROLE_ADMIN`)
    `DELETE /api/users/{id}`

*   **Assign Role to User:** (Requires `ROLE_ADMIN`)
    `POST /api/users/{userId}/roles/{roleName}`
    Example: `POST http://localhost:8081/api/users/1/roles/ROLE_ADMIN`

*   **Remove Role from User:** (Requires `ROLE_ADMIN`)
    `DELETE /api/users/{userId}/roles/{roleName}`
    Example: `DELETE http://localhost:8081/api/users/1/roles/ROLE_USER`

### 2. Journal Service (http://localhost:8082)

**Requires JWT with `ROLE_ADMIN` (or `ROLE_AUDITOR` if configured).** Use the token obtained from User Service login.

*   **Retrieve All Journaled Events (Paginated):**
    `GET /api/journal/events`
    Example: `GET http://localhost:8082/api/journal/events?page=0&size=10`

*   **Retrieve Journaled Event by ID:**
    `GET /api/journal/events/{id}`

*   **Retrieve Journaled Events by User ID (Paginated):**
    `GET /api/journal/events/user/{userId}`
    Example: `GET http://localhost:8082/api/journal/events/user/1?page=0&size=5`

## Event Flow Example

1.  Register `testuser` via User Service.
    *   User Service saves user to `user_db`.
    *   User Service publishes `USER_CREATED` event to Kafka `user-events` topic.
2.  Journal Service consumes `USER_CREATED` event.
    *   Journal Service logs the event.
    *   Journal Service persists the event to `journal_db`.
3.  Query Journal Service API `/api/journal/events` (with Admin token).
    *   You should see the `USER_CREATED` event for `testuser`.

## Stopping the System

```bash
docker-compose down -v # -v removes volumes (Kafka data, DB data)