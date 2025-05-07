Alright, let's go through the steps to run the entire system and verify that both microservices are working correctly and communicating via Kafka.

**Prerequisites:**

1.  **Java 17+ installed.**
2.  **Maven 3.6+ installed.**
3.  **Docker and Docker Compose installed and running.**
4.  **Code Cloned:** You have the complete project structure we've built.
5.  **Ports Available:** Ensure ports `2181`, `9092`, `5432`, `5433`, `8081`, `8082` are free on your machine, or adjust the `docker-compose.yml` and `application.properties` files accordingly.

**Steps to Run and Verify:**

**Step 1: Build the Applications**

Open your terminal or command prompt, navigate to the root directory of the `microservice-showcase` project (where the parent `pom.xml` is), and run:

```bash
mvn clean package -DskipTests
```

This command will:
*   Clean previous builds.
*   Compile the code for both `user-service` and `journal-service`.
*   Package them into executable JAR files (e.g., `user-service-1.0-SNAPSHOT.jar`) located in their respective `target` directories.
*   `-DskipTests` is used here to speed up the build; you'd run tests separately in a CI/CD pipeline or during development.

**Step 2: Start the Entire System with Docker Compose**

From the same root directory (`microservice-showcase`), run:

```bash
docker-compose up --build
```

This command will:
*   `--build`: Force Docker Compose to build the images for `user-service` and `journal-service` using their Dockerfiles (important if you've made code changes).
*   Start containers for:
    *   `zookeeper`
    *   `kafka` (and create the `user-events` topic)
    *   `postgres-user` (PostgreSQL for user service)
    *   `postgres-journal` (PostgreSQL for journal service)
    *   `user-service` (your Spring Boot app)
    *   `journal-service` (your other Spring Boot app)
*   You'll see a lot of log output from all the containers.

**Step 3: Monitor Service Startup**

Keep an eye on the logs. You're looking for messages indicating successful startup for both services:

*   **For User Service (typically green or INFO logs):**
    ```
    user-service-app    | ... Started UserManagementApplication in ... seconds (process running for ...)
    user-service-app    | ... Tomcat started on port(s): 8081 (http) ...
    ```
*   **For Journal Service:**
    ```
    journal-service-app | ... Started JournalServiceApplication in ... seconds (process running for ...)
    journal-service-app | ... Tomcat started on port(s): 8082 (http) ...
    journal-service-app | ... o.s.k.l.KafkaMessageListenerContainer    : journal-group: partitions assigned: [user-events-0]
    ```
    The Kafka listener message for `journal-group` on `user-events-0` is a good sign that the Journal Service has connected to Kafka and is ready to consume messages.

You can also open new terminal windows and tail the logs for specific services:

```bash
docker-compose logs -f user-service
docker-compose logs -f journal-service
docker-compose logs -f kafka
```

**Step 4: Test User Service and Event Publishing**

Use a tool like Postman, Insomnia, or `curl`.

1.  **Register a New User (User Service):**
    *   **Method:** `POST`
    *   **URL:** `http://localhost:8081/api/auth/register`
    *   **Body (JSON):**
        ```json
        {
            "username": "johndoe",
            "email": "john.doe@example.com",
            "password": "password123"
        }
        ```
    *   **Expected Response (User Service - HTTP 200 OK):**
        ```json
        {
            "id": 1, // or some other ID
            "username": "johndoe",
            "email": "john.doe@example.com",
            "roles": ["ROLE_USER"]
        }
        ```
    *   **Check User Service Logs:** You should see logs related to user creation and publishing an event to Kafka.
        ```
        user-service-app    | ... INFO c.e.userservice.service.UserService : Registering user: johndoe
        user-service-app    | ... INFO c.e.u.s.KafkaEventPublisher : Published event to user-events: UserEvent(eventType=USER_CREATED, userId=1, username=johndoe, timestamp=..., details={email=john.doe@example.com, roles=[ROLE_USER]})
        ```

2.  **Check Kafka Logs (Optional, but good for debugging):**
    You might see Kafka logs indicating a message was produced to the `user-events` topic if its logging level is verbose enough. This is harder to spot directly without specific Kafka tools.

3.  **Check Journal Service Logs:**
    *   The Journal Service should consume the event. Look for logs like:
        ```
        journal-service-app | ... INFO c.e.j.s.JournalEventConsumer : Received event from Kafka: UserEvent(eventType=USER_CREATED, userId=1, username=johndoe, timestamp=..., details={email=john.doe@example.com, roles=[ROLE_USER]})
        journal-service-app | ... INFO c.e.j.s.JournalEventConsumer : Persisted journal entry: 1
        ```

**Step 5: Test Journal Service API**

1.  **Login to Get a JWT (User Service):**
    *   First, you need an admin user to access the journal. If you haven't set one up, you can:
        *   A) Modify `UserService::registerUser` to temporarily grant `ROLE_ADMIN` to the first user or a specific username.
        *   B) Manually update the database. After registering `johndoe`:
            *   Connect to the `postgres-user` database (e.g., using `psql` inside the container or a GUI tool connected to `localhost:5432`).
            *   `docker exec -it postgres-user-db psql -U user_admin -d user_db`
            *   Run these SQL commands:
                ```sql
                -- Ensure ROLE_ADMIN exists (data.sql should have done this)
                -- INSERT INTO roles (name) VALUES ('ROLE_ADMIN') ON CONFLICT (name) DO NOTHING;
                -- Get IDs
                SELECT id FROM users WHERE username = 'johndoe'; -- e.g., returns 1
                SELECT id FROM roles WHERE name = 'ROLE_ADMIN'; -- e.g., returns 2
                -- Assign role
                INSERT INTO user_roles (user_id, role_id) VALUES (1, 2); -- Use the actual IDs
                ```
    *   Now, log in as `johndoe` (who should now also have `ROLE_ADMIN`):
        *   **Method:** `POST`
        *   **URL:** `http://localhost:8081/api/auth/login`
        *   **Body (JSON):**
            ```json
            {
                "username": "johndoe",
                "password": "password123"
            }
            ```
        *   **Expected Response (User Service - HTTP 200 OK):**
            ```json
            {
                "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huZG9lIiwicm9sZXMiOlsiUk9MRV9VU0VSIiwiUk9MRV9BRE1JTiJdLCJpYXQiOjE2Nz...", // A long JWT string
                "type": "Bearer",
                "username": "johndoe",
                "roles": ["ROLE_USER", "ROLE_ADMIN"]
            }
            ```
        *   **Copy the `token` value.**

2.  **Retrieve Journaled Events (Journal Service):**
    *   **Method:** `GET`
    *   **URL:** `http://localhost:8082/api/journal/events`
    *   **Headers:**
        *   `Authorization`: `Bearer <COPIED_JWT_TOKEN_HERE>`
    *   **Expected Response (Journal Service - HTTP 200 OK):**
        A paginated list of journal entries. You should see the `USER_CREATED` event for `johndoe`.
        ```json
        {
            "content": [
                {
                    "id": 1,
                    "eventType": "USER_CREATED",
                    "userId": 1,
                    "username": "johndoe",
                    "eventTimestamp": "2023-10-28T10:15:30.123Z", // Example timestamp
                    "detailsJson": "{\"email\":\"john.doe@example.com\",\"roles\":[\"ROLE_USER\"]}",
                    "receivedTimestamp": "2023-10-28T10:15:30.456Z" // Example timestamp
                }
                // ... other events if any
            ],
            "pageable": { ... },
            "last": true,
            "totalPages": 1,
            "totalElements": 1,
            // ... other pagination details
        }
        ```

**Step 6: Further Testing (Optional)**

*   **Update User:**
    *   `PUT http://localhost:8081/api/users/1` (with Admin JWT)
    *   Body: `{"email": "john.doe.new@example.com"}`
    *   Check User Service logs for `USER_UPDATED` event.
    *   Check Journal Service logs for consumption.
    *   Query `GET http://localhost:8082/api/journal/events` again; you should see the new event.
*   **Delete User:**
    *   `DELETE http://localhost:8081/api/users/1` (with Admin JWT)
    *   Check User Service logs for `USER_DELETED` event.
    *   Check Journal Service logs for consumption.
    *   Query `GET http://localhost:8082/api/journal/events` again.
*   **Assign/Remove Role:**
    *   `POST http://localhost:8081/api/users/1/roles/ROLE_AUDITOR` (assuming `ROLE_AUDITOR` exists or you add it).
    *   Check logs and journal.

**Troubleshooting:**

*   **Service Not Starting:** Check the logs for that specific service (`docker-compose logs -f <service_name>`). Look for stack traces or error messages related to database connections, Kafka connections, port conflicts, or configuration issues (like missing `jwt.secret`).
*   **Kafka Connection Issues:**
    *   Ensure Zookeeper is running before Kafka.
    *   Verify `SPRING_KAFKA_BOOTSTRAP_SERVERS` in `application.properties` of both services and in `docker-compose.yml` are correct (e.g., `kafka:29092` for inter-container communication, `localhost:9092` if running services outside Docker and Kafka inside).
*   **Database Connection Issues:**
    *   Check `SPRING_DATASOURCE_URL`, username, and password in `application.properties` and `docker-compose.yml`.
    *   Ensure the PostgreSQL containers are healthy.
*   **401 Unauthorized / 403 Forbidden:**
    *   Are you sending the JWT correctly in the `Authorization: Bearer <token>` header?
    *   Is the token expired? (Login again to get a new one).
    *   Does the token contain the required roles (`ROLE_ADMIN` for most journal endpoints)?
    *   Is the `jwt.secret` identical in both `user-service` and `journal-service` configurations?
*   **Event Not Reaching Journal Service:**
    *   Verify User Service published the event (check its logs).
    *   Check Kafka container logs (harder to see individual messages).
    *   Check Journal Service logs for connection errors to Kafka or deserialization errors for `UserEvent`. Ensure the `UserEvent` DTO structure is identical (or compatible) in both services.
*   **Deserialization Errors in Journal Service:**
    *   If Kafka consumer in Journal Service has trouble deserializing `UserEvent`, ensure the `UserEvent` class definition is identical in both projects (or use a shared library). Check `KafkaConsumerConfig` in Journal Service, especially `jsonDeserializer.addTrustedPackages("*");` or specific packages.

**Step 7: Stop the System**

When you're done testing:

```bash
docker-compose down
```

If you want to remove the data volumes (database data, Kafka topic data) as well:

```bash
docker-compose down -v
```

By following these steps, you can systematically run your microservices and confirm they are operating and interacting as designed.
