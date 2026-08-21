CREATE SEQUENCE hold_id_seq START 1 INCREMENT 50;

CREATE TABLE hold(
    id BIGINT NOT NULL DEFAULT nextval('hold_id_seq') PRIMARY KEY,
    item_id BIGINT NOT NULL,
    qty INT NOT NULL CHECK(qty > 0),
    hold_key VARCHAR(36) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'HELD'
                 CHECK (status IN ('HELD','COMMITTED','RELEASED','EXPIRED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);