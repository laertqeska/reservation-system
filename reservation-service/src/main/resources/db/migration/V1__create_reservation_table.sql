CREATE SEQUENCE reservation_id_seq START 1 INCREMENT 50;

CREATE TABLE reservation(
    id BIGINT NOT NULL DEFAULT nextval('reservation_id_seq') PRIMARY KEY,
    item_id BIGINT NOT NULL,
    qty INT NOT NULL CHECK (qty > 0),
    idempotency_key VARCHAR(36) NOT NULL UNIQUE,
    hold_id BIGINT,
    status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','HELD','CONFIRMED','FAILED','EXPIRED','CANCELLED')),
    failure_reason VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
)