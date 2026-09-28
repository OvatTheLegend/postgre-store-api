# PostgreStore // Persistent Product & Inventory Catalog REST API

A robust, enterprise-grade Persistent CRUD and Inventory Catalog REST API built with **Java 21 LTS**, **Spring Boot 3.3**, **PostgreSQL 16**, **Spring Data JPA & Hibernate**, **Flyway Database Migrations**, **HikariCP Connection Pooling**, **Docker Compose**, and **RFC 7807 ProblemDetail** error handling.

---

##  Features

- ** Clean Layered Architecture (`controller` → `service` → `repository` → `entity` / `dto`):**
  - Strict separation of concerns keeping presentation, validation, business transactions, data access, and domain persistence completely decoupled.
  - **Constructor Injection**: All dependencies injected through constructor parameters for immutability, thread safety, and testability without reflection.

- ** Database Migrations & Versioned DDL (`Flyway`):**
  - **Deterministic Schema Evolution (`db/migration/V1__create_products_table.sql`)**: Single source of truth for PostgreSQL schema creation with UUID primary keys, `NUMERIC(10,2)` money types, check constraints (`price >= 0`, `stock_quantity >= 0`), and B-Tree indexes.
  - **Zero Hibernate Schema Mutation (`ddl-auto: validate`)**: Strict validation-only mode ensuring code and database remain in 100% synchronized alignment without dangerous auto-generated DDL changes.

- ** High-Throughput HikariCP Connection Pooling (`application.yml`):**
  - **Pre-Warmed Connection Pool (`PostgreStoreHikariPool`)**: Maintains 10 high-performance reusable PostgreSQL database connections with aggressive timeout rules (`connection-timeout: 30s`, `idle-timeout: 10m`, `max-lifetime: 30m`) for maximum throughput.

- ** Concurrency Control & Optimistic Locking (`@Version`):**
  - **Optimistic Locking**: Dedicated `version BIGINT` column managed by Hibernate to prevent lost updates and race conditions during simultaneous concurrent updates.

- **⏱ Automated JPA Auditing (`@EnableJpaAuditing`):**
  - **`@CreatedDate` & `@LastModifiedDate`**: Automatically captures and injects UTC timestamps (`createdAt`, `updatedAt`) upon entity creation and modification with zero manual boilerplate.
  - **Modular `JpaConfig`**: JPA Auditing configuration isolated from `@SpringBootApplication` to prevent context pollution in web slice tests.

- ** Spring Data JPA Repositories & Custom JPQL Queries (`repository/`):**
  - **Derived Query Methods**: `findBySku()`, `existsBySku()`, `findByActiveTrue()`, and `findByPriceBetween()`.
  - **Custom Case-Insensitive JPQL (`@Query`)**: Type-safe substring keyword search across product name and description (`LOWER(name) LIKE ... OR LOWER(description) LIKE ...`).
  - **Dynamic Pagination & Sorting (`Pageable` & `Page<T>`)**: Efficient `LIMIT`, `OFFSET`, and `COUNT` execution for scalable catalog browsing.

- ** Immutable Java 21 Records & DTO Pattern (`dto/`):**
  - **Information Hiding**: `CreateProductRequest`, `UpdateProductRequest`, and `ProductResponse` isolate internal entity models from public API wire contracts.
  - **Static Factory Mapper (`ProductResponse.fromEntity(Product)`)**: Encapsulates entity-to-DTO transformation inside the record itself.
  - **Shallow Immutability**: Auto-generated accessors, equals, hashCode, and toString with zero boilerplate.

- ** RESTful HTTP Semantics & Location Headers (`controller/ProductController.java`):**
  - `POST /api/v1/products` → Returns **`201 Created`** with standard `Location: /api/v1/products/{id}` header.
  - `GET /api/v1/products` → Returns **`200 OK`** with paginated `Page<ProductResponse>` results.
  - `GET /api/v1/products/{id}` → Returns **`200 OK`** with single resource.
  - `GET /api/v1/products/sku/{sku}` → Returns **`200 OK`** with single resource by business key.
  - `GET /api/v1/products/search?query=...` → Returns **`200 OK`** with paginated search results.
  - `PUT /api/v1/products/{id}` → Returns **`200 OK`** with full resource update.
  - `PATCH /api/v1/products/{id}/stock?delta=...` → Returns **`200 OK`** with partial stock adjustment.
  - `DELETE /api/v1/products/{id}` → Returns **`204 No Content`** with empty response body.

- ** Jakarta Bean Validation (`@Valid`):**
  - Runtime validation constraints on incoming payloads (`@NotBlank`, `@Size(max = 255)`, `@NotNull`, `@DecimalMin("0.0")`, `@Min(0)`).
  - Guards business logic from negative prices, negative stock, blank strings, and null fields before execution.

- ** Centralized RFC 7807 Error Handling (`exception/GlobalExceptionHandler.java`):**
  - **Global Interceptor (`@RestControllerAdvice`)**: Intercepts unhandled domain exceptions across all controllers.
  - **Standardized `ProblemDetail` JSON**: Emits IETF RFC 7807 compliant error responses for `404 Not Found`, `409 Conflict`, `422 Unprocessable Entity`, and `400 Bad Request`.
  - **Field-Level Validation Error Maps**: Collects all failed fields into a structured `errors` map for frontend consumption.

- ** Comprehensive 3-Tier Testing Pyramid (25 Passing Tests):**
  - **Unit Tests (`ProductServiceTest.java`)**: 8 isolated, sub-second business logic & boundary tests using **JUnit 5**, **Mockito**, and fluent **AssertJ** assertions.
  - **Data Slice Tests (`ProductRepositoryTest.java`)**: 6 database integration tests using **`@DataJpaTest`** verifying derived queries, JPQL search, and B-Tree indexes against real PostgreSQL.
  - **Web Slice Tests (`ProductControllerTest.java`)**: 8 lightweight HTTP tests using **MockMvc** (`@WebMvcTest`) verifying routing, JSONPath payloads, HTTP headers, and RFC 7807 error structures.
  - **End-to-End Tests (`ProductIntegrationTest.java`)**: 2 full lifecycle tests using **`@SpringBootTest`** and **`TestRestTemplate`** verifying real network requests through Tomcat to PostgreSQL.
  - **Context Test (`PostgreStoreApplicationTests.java`)**: Verifies Spring Boot ApplicationContext bootstrapping and bean wiring.

---

##  Tech Stack

- **Framework:** [Spring Boot 3.3.5](https://spring.io/projects/spring-boot)
- **Language:** [Java 21 LTS](https://openjdk.org/projects/jdk/21/)
- **Database:** [PostgreSQL 16 (Alpine)](https://www.postgresql.org/)
- **ORM / Persistence:** [Spring Data JPA](https://spring.io/projects/spring-data-jpa) & [Hibernate 6](https://hibernate.org/)
- **Connection Pool:** [HikariCP](https://github.com/brettwooldridge/HikariCP)
- **Database Migrations:** [Flyway](https://flywaydb.org/)
- **Containerization:** [Docker & Docker Compose](https://www.docker.com/)
- **Build Tool:** [Maven Wrapper (mvnw)](https://maven.apache.org/)
- **Validation:** [Jakarta Bean Validation](https://beanvalidation.org/) (Hibernate Validator)
- **Testing:** [JUnit 5](https://junit.org/junit5/), [AssertJ](https://assertj.github.io/doc/), [MockMvc](https://docs.spring.io/spring-framework/reference/testing/spring-mvc-test-framework.html), [Mockito](https://site.mockito.org/), [TestRestTemplate](https://docs.spring.io/spring-boot/api/java/org/springframework/boot/test/web/client/TestRestTemplate.html)
- **Error Standard:** [RFC 7807 Problem Details](https://datatracker.ietf.org/doc/html/rfc7807)

---

##  Project Architecture

```text
src/
├── main/
│   ├── java/com/devpulse/postgrestore/
│   │   ├── PostgreStoreApplication.java         # Spring Boot main application entrypoint
│   │   ├── config/
│   │   │   └── JpaConfig.java                   # JPA Auditing configuration (@EnableJpaAuditing)
│   │   ├── controller/
│   │   │   └── ProductController.java           # REST Controller exposing 8 HTTP endpoints
│   │   ├── dto/
│   │   │   ├── CreateProductRequest.java        # Validated record for incoming POST payloads
│   │   │   ├── UpdateProductRequest.java        # Validated record for incoming PUT payloads
│   │   │   └── ProductResponse.java             # Public output record with static factory mapper
│   │   ├── entity/
│   │   │   └── Product.java                     # JPA Entity mapped to PostgreSQL products table
│   │   ├── exception/
│   │   │   ├── DuplicateResourceException.java  # Custom domain exception (409 Conflict)
│   │   │   ├── GlobalExceptionHandler.java      # @RestControllerAdvice with RFC 7807 ProblemDetail
│   │   │   ├── InsufficientStockException.java  # Custom domain exception (422 Unprocessable Entity)
│   │   │   └── ResourceNotFoundException.java   # Custom domain exception (404 Not Found)
│   │   ├── repository/
│   │   │   └── ProductRepository.java           # Spring Data JPA Repository with derived & JPQL queries
│   │   └── service/
│   │       ├── ProductService.java              # Service interface
│   │       └── ProductServiceImpl.java          # Business logic implementation & @Transactional
│   └── resources/
│       ├── application.yml                      # HikariCP, JPA, Flyway, and PostgreSQL configuration
│       └── db/migration/
│           └── V1__create_products_table.sql    # Flyway SQL migration script
└── test/
    └── java/com/devpulse/postgrestore/
        ├── PostgreStoreApplicationTests.java    # ApplicationContext boot verification test
        ├── ProductIntegrationTest.java          # Full End-to-End integration tests (TestRestTemplate)
        ├── controller/
        │   └── ProductControllerTest.java       # MockMvc web slice tests (Endpoints, 400, 404)
        ├── repository/
        │   └── ProductRepositoryTest.java       # @DataJpaTest repository slice tests (Real PostgreSQL)
        └── service/
            └── ProductServiceTest.java          # Unit tests with JUnit 5, Mockito & AssertJ
```

---

##  Database Schema (`products` table)

```sql
CREATE TABLE products (
    id UUID PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    stock_quantity INTEGER NOT NULL DEFAULT 0 CHECK (stock_quantity >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Query optimization B-Tree indexes
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_active ON products(active);
CREATE INDEX idx_products_price ON products(price);
```

---

##  REST API Endpoints

Base URL: `http://localhost:8080/api/v1/products`

| Method | Endpoint | Description | Status Code | Response |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/products` | Create a new product | `201 Created` | `ProductResponse` + `Location` Header |
| `GET` | `/api/v1/products` | Retrieve paginated products (`?page=0&size=10&activeOnly=true`) | `200 OK` | `Page<ProductResponse>` |
| `GET` | `/api/v1/products/{id}` | Retrieve product by UUID | `200 OK` | `ProductResponse` (or `404 ProblemDetail`) |
| `GET` | `/api/v1/products/sku/{sku}` | Retrieve product by SKU | `200 OK` | `ProductResponse` (or `404 ProblemDetail`) |
| `GET` | `/api/v1/products/search?query=...` | Search products by keyword (paginated) | `200 OK` | `Page<ProductResponse>` |
| `PUT` | `/api/v1/products/{id}` | Full update product by UUID | `200 OK` | `ProductResponse` (or `404 ProblemDetail`) |
| `PATCH` | `/api/v1/products/{id}/stock?delta=...` | Adjust inventory stock quantity | `200 OK` | `ProductResponse` (or `422 ProblemDetail`) |
| `DELETE` | `/api/v1/products/{id}` | Delete product by UUID | `204 No Content` | Empty Body (or `404 ProblemDetail`) |

---

##  Getting Started

### Prerequisites
- **Java JDK 21+** (Temurin / OpenJDK)
- **Docker & Docker Compose**
- **Git**

### Installation & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/postgre-store.git
   cd postgre-store
   ```

2. Start the PostgreSQL 16 database in Docker:
   ```bash
   docker compose up -d
   ```

3. Run the full 3-tier automated test suite:
   ```bash
   # Windows PowerShell
   .\mvnw.cmd test

   # macOS / Linux
   ./mvnw test
   ```

4. Start the Spring Boot application:
   ```bash
   # Windows PowerShell
   .\mvnw.cmd spring-boot:run

   # macOS / Linux
   ./mvnw spring-boot:run
   ```

5. The API will be live and ready for requests at:
   ```text
   http://localhost:8080/api/v1/products
   ```

6. Build production executable JAR:
   ```bash
   # Windows PowerShell
   .\mvnw.cmd clean package
   java -jar target/postgre-store-0.0.1-SNAPSHOT.jar
   ```
