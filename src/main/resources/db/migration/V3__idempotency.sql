CREATE TABLE idempotency_keys (
    key_hash VARCHAR(64) PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    fingerprint VARCHAR(64) NOT NULL,
    code VARCHAR(32) NOT NULL REFERENCES links(code)
);
