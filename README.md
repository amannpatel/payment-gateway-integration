# Payment Gateway Integration Simulator

Production-style Spring Boot backend that simulates the day-to-day responsibilities of a Payment Gateway Integration Engineer in an e-commerce environment. The project uses Razorpay semantics and webhook behavior, but the backend is structured around a gateway strategy abstraction so the same orchestration layer can later support Cybersource, MPGS, N-Genius, or other gateways.

The focus is operational realism rather than basic CRUD. Orders, payment attempts, lifecycle transitions, raw webhooks, idempotency, retries, troubleshooting APIs, support tickets, and failure simulations are all first-class parts of the design.

## What This Project Covers

- Merchant-facing APIs for payment order creation, payment initiation, and payment status lookup
- Transaction persistence for orders, attempts, lifecycle history, raw webhook deliveries, retries, and merchant issues
- Gateway abstraction using a strategy-style registry with a Razorpay implementation
- Webhook signature verification, async processing, idempotency, audit logging, and duplicate delivery handling
- Retry scheduler for failed status propagation and delayed webhook dispatch simulation
- Internal operations APIs for reconciliation, history lookup, webhook log inspection, and issue simulation
- Merchant support issue tracking with Excel export for daily issue reporting
- Correlation ID tracing and structured key-value logs for API, gateway, and webhook flows

## Architecture

Request flow follows a clean layered structure:

`Controller -> Service -> Repository -> Gateway Client`

Main modules:

- `controller`: Merchant APIs, internal ops APIs, webhook receiver, support APIs
- `service`: Payment orchestration, lifecycle transitions, webhook processing, retry worker, simulations, troubleshooting, issue management
- `repository`: JPA persistence for operational entities
- `gateway`: Gateway abstraction and Razorpay simulator/client implementation
- `entity`: Payment order, transaction, lifecycle history, webhook audit, processed event, retry task, merchant issue
- `db/migration`: Flyway-managed MySQL schema

## Payment Lifecycle

The project tracks the following canonical lifecycle:

- `CREATED`
- `AUTHORIZED`
- `CAPTURED`
- `FAILED`
- `REFUNDED`

State transitions are stored in `payment_status_history`, including transition source and reference so operational teams can explain why a payment moved state.

## Failure And Ops Scenarios

The system can simulate production-style payment problems instead of only happy-path flows.

- `SUCCESS`: merchant sees `AUTHORIZED`, then a delayed `payment.captured` webhook closes the loop
- `AUTHORIZE_ONLY`: stays in `AUTHORIZED` to mimic delayed capture flows
- `FAIL`: payment fails and a `payment.failed` webhook is generated
- `GATEWAY_TIMEOUT`: gateway actually succeeds, but the merchant platform initially sees `FAILED` until reconciliation runs
- `PARTIAL_FAILURE`: gateway captured payment, but local propagation fails and requires retry repair
- `SUCCESS_DB_NOT_UPDATED`: successful payment with deliberately stale local state for debugging exercises
- Duplicate webhook delivery: same event ID sent twice, only first one is processed
- Delayed webhook delivery: event queued to simulate late callback arrival
- Payment success but merchant sees failed: local state drift plus later reconciliation

## Database Schema

Schema is managed via Flyway in [src/main/resources/db/migration/V1__initial_schema.sql](d:\Personal Projects\payment-gateway-integration\src\main\resources\db\migration\V1__initial_schema.sql).

Core tables:

- `payment_orders`: merchant order, amount, currency, gateway, local order state
- `payment_transactions`: individual payment attempts and gateway references
- `payment_status_history`: immutable lifecycle transition audit
- `webhook_events`: every webhook delivery, including duplicates and invalid signatures
- `processed_events`: idempotency ledger for webhook and event processing
- `retry_tasks`: queued reconciliation and delayed webhook tasks, including dead-letter behavior
- `merchant_issues`: support tickets raised by merchants

## Running The Project

Prerequisites:

- Java 21
- Maven 3.9+
- MySQL 8+ for the default profile

### Default MySQL Profile

Set database credentials through environment variables if needed:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/payment_gateway_ops?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="root"
mvn spring-boot:run
```

### Local Demo Profile

For quick local exploration without MySQL, run the in-memory H2 profile:

```powershell
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- API reference: [docs/api-reference.md](d:\Personal Projects\payment-gateway-integration\docs\api-reference.md)
- Postman collection: [postman/payment-gateway-integration.postman_collection.json](d:\Personal Projects\payment-gateway-integration\postman\payment-gateway-integration.postman_collection.json)

## Suggested End-To-End Flow

1. Create a merchant order using `POST /api/v1/payments/orders`.
2. Initiate payment using `POST /api/v1/payments/orders/{merchantOrderId}/initiate`.
3. Check merchant-visible status using `GET /api/v1/payments/orders/{merchantOrderId}/status`.
4. Inspect operational details using `GET /api/internal/transactions/search` or `GET /api/internal/orders/{merchantOrderId}/history`.
5. Review webhook deliveries using `GET /api/internal/webhooks`.
6. Inspect reconciliation queue using `GET /api/internal/retries`.
7. Raise and track a merchant issue through `POST /api/v1/support/issues`.

## Troubleshooting Playbook

When a merchant reports that payment succeeded but their platform shows failed:

1. Search by merchant order or payment ID through `/api/internal/transactions/search`.
2. Compare `localOrderStatus` against `gatewayObservedStatus`.
3. Inspect `webhookEvents` to confirm whether the gateway callback arrived, was delayed, or was treated as duplicate.
4. Review `/api/internal/retries` to confirm whether reconciliation was queued or moved to dead-letter.
5. If needed, trigger `payment-success-db-miss`, duplicate webhook, or delayed webhook simulations to reproduce the case.
6. Open a merchant issue and track investigation status through the support module.

## Observability

- Every request gets an `X-Correlation-Id` header for traceability
- Console logs include correlation ID, logger, thread, and structured key-value message fields
- Actuator endpoints exposed: `health`, `info`, `metrics`, `loggers`

## Testing

```powershell
mvn test
```

Included test coverage verifies application startup and a representative merchant order plus initiate-payment flow through MockMvc.