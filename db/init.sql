CREATE TABLE IF NOT EXISTS crypto_operation (
    id         BIGSERIAL PRIMARY KEY,
    service    TEXT        NOT NULL,
    operation  TEXT        NOT NULL,
    input      BYTEA,
    output     BYTEA,
    status     TEXT        NOT NULL,
    detail     TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_crypto_operation_service   ON crypto_operation (service);
CREATE INDEX IF NOT EXISTS idx_crypto_operation_operation ON crypto_operation (operation);
CREATE INDEX IF NOT EXISTS idx_crypto_operation_created   ON crypto_operation (created_at);
