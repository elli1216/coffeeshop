# ☕ Coffeeshop — Backend (Spring Boot API)

REST API for categories, products, and orders. Built with **Spring Boot 4**, **Java 21**, **Spring Data JPA**, and **MySQL 8**.

Base package: `com.accenture.cofeeshop`

---

## 1. Tech stack

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-webmvc` | REST controllers |
| `spring-boot-starter-data-jpa` | Entities and repositories (Hibernate) |
| `spring-boot-starter-validation` | `@NotBlank`, `@Positive`, `@Valid` |
| `spring-boot-starter-actuator` | `/actuator/health` (used by the smoke test) |
| `com.mysql:mysql-connector-j` (runtime) | MySQL JDBC driver |
| `lombok` (optional) | Getters/setters/constructors |
| `spring-boot-devtools` (runtime, optional) | Auto-restart during development |
| `*-test` starters | JUnit 5 tests |

> Use `com.mysql:mysql-connector-j`. The old `mysql:mysql-connector-java` coordinates fail on Spring Boot 3+.

---

## 2. Architecture

```text
HTTP request
   │
   ▼
controller   → HTTP only: routes, status codes, @Valid
   │
   ▼
service      → business rules, @Transactional, entity ↔ DTO mapping
   │
   ▼
repository   → Spring Data JPA (queries derived from method names)
   │
   ▼
MySQL (coffeeshop database)
```

```text
src/main/java/com/accenture/cofeeshop/
├── config/       WebConfig (CORS)
├── controller/   CategoryController, ProductController, OrderController
├── dto/          category/, product/, order/ request & response records
├── exception/    ResourceNotFoundException, BusinessException, GlobalExceptionHandler
├── model/        Category, Product, CustomerOrder, OrderItem, OrderStatus
├── repository/   CategoryRepository, ProductRepository, CustomerOrderRepository
└── service/      CategoryService, ProductService, OrderService
```

> DTOs live in **subpackages**. Import them explicitly (e.g. `dto.order.*`) — a `dto.*` wildcard does not include subpackages.

---

## 3. Data model

| Entity | Table | Key fields | Relationships |
|---|---|---|---|
| `Category` | `categories` | `name` (unique), `description` | 1 → many products |
| `Product` | `products` | `name`, `price` (`BigDecimal`), `available` | many → 1 category |
| `CustomerOrder` | `orders` | `customerName`, `status`, `totalAmount`, `createdAt` | 1 → many items (cascade) |
| `OrderItem` | `order_items` | `quantity`, `unitPrice` | many → 1 order, many → 1 product |
| `OrderStatus` | enum | `PENDING`, `PREPARING`, `COMPLETED`, `CANCELLED` | — |

Design decisions:
- **`BigDecimal` for money** — avoids floating-point rounding errors.
- **`unitPrice` stored on `OrderItem`** — old orders keep the price charged at the time.
- **Entity named `CustomerOrder`, table `orders`** — `ORDER` is a reserved SQL word.
- **`@Getter`/`@Setter` instead of `@Data`** — avoids infinite `toString`/`hashCode` loops on bidirectional relationships.
- **DTOs instead of entities in responses** — prevents lazy-loading errors and JSON loops.

---

## 4. Business rules (OrderService)

- Every ordered product must exist and be `available`.
- Prices are copied from the product; **the total is calculated on the server**.
- Status transitions:

```text
PENDING ──► PREPARING ──► COMPLETED
   │            │
   └────────────┴──► CANCELLED
COMPLETED and CANCELLED are final.
```

---

## 5. API endpoints

Base URL: `http://localhost:8081`

| Method | Endpoint | Description | Success |
|---|---|---|---|
| GET | `/actuator/health` | Health check | 200 |
| GET | `/api/categories` | List categories | 200 |
| GET | `/api/categories/{id}` | Get a category | 200 |
| POST | `/api/categories` | Create a category | 201 |
| PUT | `/api/categories/{id}` | Update a category | 200 |
| DELETE | `/api/categories/{id}` | Delete a category | 204 |
| GET | `/api/products` | List products (`?available=true`, `?categoryId=1`) | 200 |
| GET | `/api/products/{id}` | Get a product | 200 |
| POST | `/api/products` | Create a product | 201 |
| PUT | `/api/products/{id}` | Update a product | 200 |
| DELETE | `/api/products/{id}` | Delete a product | 204 |
| GET | `/api/orders` | List orders (`?status=PENDING`) | 200 |
| GET | `/api/orders/{id}` | Get an order | 200 |
| POST | `/api/orders` | Place an order | 201 |
| PATCH | `/api/orders/{id}/status` | Change status | 200 |
| PATCH | `/api/orders/{id}/cancel` | Cancel an order | 200 |

### Sample requests

```json
POST /api/categories
{ "name": "Coffee", "description": "Hot and iced coffee drinks" }

POST /api/products
{ "name": "Latte", "description": "Espresso with steamed milk", "price": 150.00, "categoryId": 1 }

POST /api/orders
{ "customerName": "Darl", "items": [ { "productId": 1, "quantity": 2 } ] }

PATCH /api/orders/1/status
{ "status": "PREPARING" }
```

---

## 6. Error handling

`GlobalExceptionHandler` (`@RestControllerAdvice`) returns Spring `ProblemDetail` JSON and **never exposes stack traces**.

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `NoResourceFoundException` / `NoHandlerFoundException` (unknown URL) | 404 |
| `BusinessException` (rule violation) | 400 |
| `MethodArgumentNotValidException` (validation) | 400 + `errors` map |
| `HttpMessageNotReadableException` (bad JSON / enum) | 400 |
| `MethodArgumentTypeMismatchException` (e.g. `/orders/abc`) | 400 |
| `HttpRequestMethodNotSupportedException` | 405 |
| `DataIntegrityViolationException` | 409 |
| Any other `Exception` | 500 (logged server-side) |

Example:

```json
{
  "status": 400,
  "detail": "Validation failed",
  "errors": { "name": "must not be blank", "price": "must not be null" }
}
```

---

## 7. Configuration

### `src/main/resources/application.properties`

```properties
server.port=8081                                   # Jenkins uses 8080

spring.datasource.url=jdbc:mysql://localhost:3307/coffeeshop
spring.datasource.username=coffee
spring.datasource.password=coffee
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

server.error.include-stacktrace=never
server.error.include-exception=false
server.error.include-message=never
```

| Environment | Datasource URL | Set by |
|---|---|---|
| Local (IntelliJ) | `jdbc:mysql://localhost:3307/coffeeshop` | `application.properties` |
| CI tests / deployed container | `jdbc:mysql://db:3306/coffeeshop` | `SPRING_DATASOURCE_URL` env var (overrides the property) |

### CORS (`config/WebConfig`)

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:5173", "http://localhost:8083")
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
```

---

## 8. Running locally

**Prerequisites:** JDK 21, Maven, Docker MySQL running (`~/gitea-docker`), `coffeeshop` DB and `coffee` user created.

```powershell
mvn clean verify          # build + tests
mvn spring-boot:run       # or run CofeeshopApplication from IntelliJ
```

### Auto-reload (DevTools) in IntelliJ

1. Settings → Build, Execution, Deployment → Compiler → **Build project automatically**
2. Settings → Advanced Settings → **Allow auto-make to start even if developed application is currently running**
3. Restart IntelliJ.

---

## 9. Testing

- Tests in `src/test/java` run automatically in the pipeline via `mvn verify` (Maven Surefire).
- Class names must match `*Test`, `*Tests`, `Test*`, or `*TestCase`.
- `@SpringBootTest` tests need MySQL; plain Mockito unit tests do not.
- A failing test fails the **Backend: Build & Test** stage.

---

## 10. Code quality (SonarQube)

Analyzed by the **SonarScanner for Maven** — no `sonar-project.properties` needed. Sources, tests, classes, and libraries are read from `pom.xml`; the key is passed in the Jenkinsfile:

```bash
mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
    -Dsonar.projectKey=coffeeshop-backend \
    -Dsonar.projectName=coffeeshop-backend
```

> Test coverage shows 0% until the JaCoCo plugin is added to `pom.xml`.

---

## 11. Docker image (`Dockerfile`)

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

The jar is built by the pipeline's `mvn verify` stage before the image is built in the Deploy stage.
