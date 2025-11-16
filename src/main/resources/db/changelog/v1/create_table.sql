CREATE TABLE IF NOT EXISTS acq_operation (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               client_id BIGINT NOT NULL,
                               state VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_acq_operation_client_id ON acq_operation(client_id);

CREATE INDEX IF NOT EXISTS idx_acq_operation_created_at ON acq_operation(created_at);

CREATE INDEX IF NOT EXISTS idx_acq_operation_state ON acq_operation(state);