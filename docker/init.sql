-- DataPet database schema

-- Logs table: stores all ingested log entries
CREATE TABLE logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    service VARCHAR(255) NOT NULL,
    level VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    context JSONB DEFAULT '{}',
    ingested_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for common query patterns
CREATE INDEX idx_logs_timestamp ON logs (timestamp DESC);
CREATE INDEX idx_logs_service ON logs (service);
CREATE INDEX idx_logs_level ON logs (level);
CREATE INDEX idx_logs_service_level ON logs (service, level);
CREATE INDEX idx_logs_context ON logs USING GIN (context);

-- Aggregations table: stores pre-computed metrics
CREATE TABLE aggregations (
    id BIGSERIAL PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    service VARCHAR(255) NOT NULL,
    level VARCHAR(50) NOT NULL,
    count INTEGER NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (window_start, window_end, service, level)
);

CREATE INDEX idx_aggregations_window ON aggregations (window_start DESC);
CREATE INDEX idx_aggregations_service ON aggregations (service);

-- Alert rules table: stores user-defined alert rules
CREATE TABLE alert_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    condition JSONB NOT NULL,
    action JSONB NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Triggered alerts table: stores alert history
CREATE TABLE alerts (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT REFERENCES alert_rules(id),
    rule_name VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    context JSONB DEFAULT '{}',
    triggered_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_alerts_triggered_at ON alerts (triggered_at DESC);
CREATE INDEX idx_alerts_rule_id ON alerts (rule_id);
