
CREATE SEQUENCE inventory_item_id_seq START 1 INCREMENT 50;


CREATE TABLE inventory_items(
    id BIGINT NOT NULL DEFAULT nextval('inventory_item_id_seq') PRIMARY KEY,
    sku VARCHAR(50) NOT NULL UNIQUE,
    total INT NOT NULL CHECK(total >= 0),
    available INT NOT NULL CHECK(available >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

