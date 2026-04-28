CREATE TABLE payment_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id VARCHAR(64) NOT NULL,
    merchant_order_id VARCHAR(128) NOT NULL,
    gateway VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    gateway_order_id VARCHAR(128) NULL,
    latest_payment_id VARCHAR(128) NULL,
    description VARCHAR(512) NULL,
    customer_email VARCHAR(255) NULL,
    customer_phone VARCHAR(32) NULL,
    failure_code VARCHAR(64) NULL,
    failure_reason VARCHAR(512) NULL,
    correlation_id VARCHAR(64) NULL,
    metadata_json TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_payment_orders PRIMARY KEY (id),
    CONSTRAINT uk_payment_orders_merchant_order UNIQUE (merchant_id, merchant_order_id)
);

CREATE INDEX idx_payment_orders_merchant_order_id ON payment_orders (merchant_order_id);
CREATE INDEX idx_payment_orders_latest_payment_id ON payment_orders (latest_payment_id);

CREATE TABLE payment_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_order_id BIGINT NOT NULL,
    attempt_number INT NOT NULL,
    gateway VARCHAR(32) NOT NULL,
    gateway_order_id VARCHAR(128) NULL,
    gateway_payment_id VARCHAR(128) NULL,
    status VARCHAR(32) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    payment_method VARCHAR(64) NULL,
    authorization_id VARCHAR(128) NULL,
    failure_code VARCHAR(64) NULL,
    failure_reason VARCHAR(512) NULL,
    gateway_reference VARCHAR(128) NULL,
    gateway_raw_response TEXT NULL,
    last_processed_event_key VARCHAR(255) NULL,
    authorized_at DATETIME(6) NULL,
    captured_at DATETIME(6) NULL,
    refunded_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT fk_payment_transactions_order FOREIGN KEY (payment_order_id) REFERENCES payment_orders (id),
    CONSTRAINT uk_payment_transactions_gateway_payment_id UNIQUE (gateway_payment_id)
);

CREATE INDEX idx_payment_transactions_payment_order_id ON payment_transactions (payment_order_id);
CREATE INDEX idx_payment_transactions_gateway_payment_id ON payment_transactions (gateway_payment_id);

CREATE TABLE payment_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    payment_transaction_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    source_reference VARCHAR(255) NULL,
    notes VARCHAR(512) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_payment_status_history PRIMARY KEY (id),
    CONSTRAINT fk_payment_status_history_transaction FOREIGN KEY (payment_transaction_id) REFERENCES payment_transactions (id)
);

CREATE INDEX idx_payment_status_history_transaction_id ON payment_status_history (payment_transaction_id);

CREATE TABLE webhook_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    gateway VARCHAR(32) NOT NULL,
    gateway_event_id VARCHAR(128) NOT NULL,
    delivery_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    signature_valid BIT(1) NOT NULL,
    payment_id VARCHAR(128) NULL,
    order_id VARCHAR(128) NULL,
    payload LONGTEXT NOT NULL,
    payload_hash VARCHAR(128) NOT NULL,
    processing_status VARCHAR(32) NOT NULL,
    processing_error VARCHAR(1024) NULL,
    duplicate_delivery BIT(1) NOT NULL,
    correlation_id VARCHAR(64) NULL,
    received_at DATETIME(6) NOT NULL,
    processed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_webhook_events PRIMARY KEY (id)
);

CREATE INDEX idx_webhook_events_gateway_event_id ON webhook_events (gateway_event_id);
CREATE INDEX idx_webhook_events_order_id ON webhook_events (order_id);
CREATE INDEX idx_webhook_events_payment_id ON webhook_events (payment_id);

CREATE TABLE processed_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    payload_hash VARCHAR(128) NULL,
    processed_at DATETIME(6) NOT NULL,
    correlation_id VARCHAR(64) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_processed_events PRIMARY KEY (id),
    CONSTRAINT uk_processed_events_event_key UNIQUE (event_key)
);

CREATE TABLE merchant_issues (
    id BIGINT NOT NULL AUTO_INCREMENT,
    merchant_id VARCHAR(64) NOT NULL,
    payment_order_id BIGINT NULL,
    payment_id VARCHAR(128) NULL,
    issue_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    description VARCHAR(2048) NOT NULL,
    assigned_to VARCHAR(128) NULL,
    resolution_notes VARCHAR(2048) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_merchant_issues PRIMARY KEY (id),
    CONSTRAINT fk_merchant_issues_order FOREIGN KEY (payment_order_id) REFERENCES payment_orders (id)
);

CREATE INDEX idx_merchant_issues_status ON merchant_issues (status);
CREATE INDEX idx_merchant_issues_created_at ON merchant_issues (created_at);

CREATE TABLE retry_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    correlation_id VARCHAR(64) NULL,
    payload LONGTEXT NULL,
    attempt_count INT NOT NULL,
    max_attempts INT NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    last_attempt_at DATETIME(6) NULL,
    last_error VARCHAR(1024) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_retry_tasks PRIMARY KEY (id)
);

CREATE INDEX idx_retry_tasks_status_next_attempt ON retry_tasks (status, next_attempt_at);