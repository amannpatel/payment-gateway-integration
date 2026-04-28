# API Reference

## Merchant APIs

### Create Payment Order

- Method: `POST`
- Path: `/api/v1/payments/orders`

Example body:

```json
{
  "merchantId": "merchant-demo",
  "merchantOrderId": "ORD-1001",
  "gateway": "RAZORPAY",
  "amount": 1499.99,
  "currency": "INR",
  "description": "Cart checkout",
  "customerEmail": "buyer@example.com",
  "customerPhone": "+919999999999",
  "metadata": {
    "cartId": "CART-42",
    "channel": "web"
  }
}
```

### Initiate Payment

- Method: `POST`
- Path: `/api/v1/payments/orders/{merchantOrderId}/initiate`

Example body:

```json
{
  "paymentMethod": "card",
  "scenario": "SUCCESS",
  "webhookDelaySeconds": 5
}
```

Supported scenarios:

- `SUCCESS`
- `AUTHORIZE_ONLY`
- `FAIL`
- `GATEWAY_TIMEOUT`
- `PARTIAL_FAILURE`
- `SUCCESS_DB_NOT_UPDATED`

### Get Payment Status

- Method: `GET`
- Path: `/api/v1/payments/orders/{merchantOrderId}/status`

## Webhook API

### Receive Razorpay Webhook

- Method: `POST`
- Path: `/api/v1/webhooks/razorpay`
- Required headers:
- `X-Razorpay-Signature`
- `X-Razorpay-Event-Id`
- Optional header:
- `X-Razorpay-Delivery-Id`

The payload is stored raw for audit, signature-checked, and processed asynchronously by default.

## Internal Operations APIs

### Search Transactions

- Method: `GET`
- Path: `/api/internal/transactions/search`
- Query params:
- `merchantOrderId`
- `paymentId`

Returns merchant-visible state, gateway-observed state, attempts, lifecycle history, and webhook logs.

### Order History

- Method: `GET`
- Path: `/api/internal/orders/{merchantOrderId}/history`

### Webhook Logs

- Method: `GET`
- Path: `/api/internal/webhooks`
- Query params:
- `orderId`
- `paymentId`

### Retry Queue

- Method: `GET`
- Path: `/api/internal/retries`

## Simulation APIs

### Duplicate Webhook

- Method: `POST`
- Path: `/api/internal/simulations/duplicate-webhook`

Example body:

```json
{
  "merchantOrderId": "ORD-1001",
  "eventType": "payment.captured"
}
```

### Delayed Webhook

- Method: `POST`
- Path: `/api/internal/simulations/delayed-webhook`

Example body:

```json
{
  "merchantOrderId": "ORD-1001",
  "eventType": "payment.captured",
  "delaySeconds": 20
}
```

### Payment Success But DB Not Updated

- Method: `POST`
- Path: `/api/internal/simulations/payment-success-db-miss`

Example body:

```json
{
  "merchantOrderId": "ORD-1001",
  "note": "Force stale merchant-visible state"
}
```

## Support APIs

### Raise Merchant Issue

- Method: `POST`
- Path: `/api/v1/support/issues`

Example body:

```json
{
  "merchantId": "merchant-demo",
  "merchantOrderId": "ORD-1001",
  "paymentId": "pay_cap_xxx",
  "issueType": "PAYMENT_NOT_UPDATED",
  "summary": "Customer was charged but store shows failed",
  "description": "Investigate stale state and callback timing"
}
```

### Update Issue Status

- Method: `PATCH`
- Path: `/api/v1/support/issues/{issueId}/status`

### List Issues

- Method: `GET`
- Path: `/api/v1/support/issues`
- Query param:
- `status`

### Export Daily Issues

- Method: `GET`
- Path: `/api/v1/support/issues/export/daily?date=2026-04-28`