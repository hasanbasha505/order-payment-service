# Order Payment Lifecycle Service

A production-grade payment lifecycle management service for food delivery/restaurant platforms.

## Overview

This service handles the complete payment flow from order creation through authorization, capture, and refunds. It is designed with high reliability, concurrency safety, and financial compliance in mind.

### Key Features

- **Order Management**: Create orders with automatic payment initialization
- **Payment Authorization**: Idempotent payment authorization with provider integration
- **Capture Prevention**: Double-capture protection via pessimistic locking and idempotency
- **Partial/Full Refunds**: Safe refund processing with balance validation
- **Reconciliation**: Timezone-aware daily settlement reports
- **Event Publishing**: Transactional outbox pattern for reliable Kafka integration

## Tech Stack

- **Language**: Java 21
- **Framework**: Spring Boot 3.3
- **Database**: PostgreSQL 15
- **Migrations**: Flyway
- **Message Broker**: Apache Kafka
- **Documentation**: OpenAPI 3.0 (Swagger)
- **Resilience**: Resilience4j (Circuit Breaker, Retry)
- **Testing**: JUnit 5, Testcontainers, AssertJ

## Quick Start

### Prerequisites

- Java 21
- Docker & Docker Compose
- Maven 3.9+

### Run with Docker Compose

```bash
# Clone and navigate to project
cd order-payment-service

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f payment-service

# Stop services
docker-compose down
```

### Run Locally (Development)

```bash
# Start dependencies only
docker-compose up -d postgres kafka zookeeper

# Run the application
./mvnw spring-boot:run

# Or build and run
./mvnw clean package -DskipTests
java -jar target/order-payment-service-1.0.0.jar
```

### Access Points

- **API**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/prometheus

## API Documentation

### Order Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders` | Create a new order |
| GET | `/api/v1/orders/{orderId}` | Get order with payment details |
| GET | `/api/v1/orders/by-number/{orderNumber}` | Get order by order number |

### Payment Endpoints (Nested under Orders)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/orders/{orderId}/payment/authorize` | Authorize payment |
| POST | `/api/v1/orders/{orderId}/payment/capture` | Capture authorized payment |
| POST | `/api/v1/orders/{orderId}/payment/refund` | Refund captured payment |
| GET | `/api/v1/orders/{orderId}/payment` | Get payment details |
| GET | `/api/v1/orders/{orderId}/payment/transactions` | Get payment with transaction history |

### Reconciliation Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/reconciliation/generate` | Generate daily reconciliation report |
| GET | `/api/v1/reconciliation/reports/{reportId}` | Get report by ID |
| GET | `/api/v1/reconciliation/reports?restaurantId=` | List reports by restaurant |
| GET | `/api/v1/reconciliation/reports/by-date?reportDate=` | Get reports for a specific date |
| GET | `/api/v1/reconciliation/reports/mismatches` | Get all unresolved mismatches |

### Required Headers

All write operations require:
```
Idempotency-Key: <unique-key>
```

### Example: Complete Payment Flow

```bash
# 1. Create Order (returns orderId in response)
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: order-$(date +%s)" \
  -d '{
    "restaurantId": "550e8400-e29b-41d4-a716-446655440001",
    "customerId": "660e8400-e29b-41d4-a716-446655440001",
    "totalAmount": 2500,
    "currencyCode": "USD"
  }'

# 2. Authorize Payment (use orderId from step 1)
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/payment/authorize \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: auth-$(date +%s)" \
  -d '{
    "amount": 2500,
    "paymentMethod": "CARD",
    "paymentToken": "tok_visa_4242"
  }'

# 3. Capture Payment
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/payment/capture \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: capture-$(date +%s)" \
  -d '{"amount": 2500}'

# 4. Partial Refund
curl -X POST http://localhost:8080/api/v1/orders/{orderId}/payment/refund \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: refund-$(date +%s)" \
  -d '{
    "amount": 1000,
    "reason": "Customer request"
  }'

# 5. Get Order with Payment Details
curl http://localhost:8080/api/v1/orders/{orderId}
```

## Architecture

### Design Principles

1. **Correctness over Availability**: For monetary operations, we prioritize consistency
2. **Idempotency at Every Layer**: HTTP filter, service layer, database constraints
3. **Immutable Audit Trail**: Transaction log is append-only
4. **Short Transactions**: External calls happen outside DB transactions

### Concurrency Safety

```
┌─────────────────────────────────────────────────────────────┐
│                   3-TIER IDEMPOTENCY                        │
│                                                              │
│  HTTP Filter → Check idempotency_keys table                 │
│       ↓                                                      │
│  Service Layer → Check payment_transactions by idempotency  │
│       ↓                                                      │
│  Database → Unique constraint on (payment_id, type, key)    │
└─────────────────────────────────────────────────────────────┘
```

### Database Schema

- `restaurants` - Restaurant with timezone info
- `orders` - Customer orders
- `payments` - Payment state machine
- `payment_transactions` - Immutable transaction ledger
- `idempotency_keys` - HTTP-level request deduplication
- `outbox_events` - Transactional event publishing

### Payment State Machine

```
PENDING → AUTHORIZED → CAPTURED → PARTIALLY_REFUNDED → FULLY_REFUNDED
    ↓           ↓           ↓
AUTH_FAILED  CANCELLED  CAPTURE_FAILED
```

## Design Decisions

### 1. BIGINT for Money
- Stores amounts in smallest currency unit (cents)
- Avoids floating-point precision issues
- Industry standard (Stripe, Square)

### 2. Pessimistic Locking + READ_COMMITTED
- `SELECT FOR UPDATE` prevents concurrent modifications
- READ_COMMITTED isolation avoids serialization errors while locks ensure safety
- Idempotency keys provide additional protection against retries

### 3. Transactional Outbox Pattern
- Events written in same DB transaction as business data
- Background publisher reads and publishes to Kafka
- Guarantees zero event loss

### 4. Single Payment per Order
- Simplifies state management
- Covers 95%+ of food delivery use cases
- Extensible to split payments if needed

## Testing

```bash
# Run all tests
./mvnw test

# Run with coverage report
./mvnw test jacoco:report

# Integration tests only
./mvnw test -Dtest="*IntegrationTest"
```

### Test Coverage Targets

| Category | Target |
|----------|--------|
| Unit Tests | >80% |
| Integration Tests | >70% |
| Concurrency Tests | 100% of write operations |

## Production Deployment

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | payments |
| `DB_USER` | Database user | postgres |
| `DB_PASSWORD` | Database password | - |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka servers | localhost:9092 |
| `SPRING_PROFILES_ACTIVE` | Active profile | - |

### Health Monitoring

- `/actuator/health` - Overall health status
- `/actuator/health/db` - Database connectivity
- `/actuator/health/circuitBreakers` - Circuit breaker status
- `/actuator/prometheus` - Prometheus metrics

### Key Metrics

- `payment.capture.total` - Capture operations count
- `payment.refund.total` - Refund operations count
- `db.transaction.duration` - Transaction duration histogram
- `resilience4j.circuitbreaker.state` - Circuit breaker states

## License

MIT License
