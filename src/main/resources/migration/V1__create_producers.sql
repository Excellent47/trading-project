-- Добавить миграцию Flyway
CREATE TABLE producers (
    id UUID PRIMARY KEY,
    product_id BIGINT NOT NULL,
    producer_name VARCHAR(255) NOT NULL,
    price INTEGER NOT NULL,
    created TIMESTAMP NOT NULL,
    updated TIMESTAMP NULL
);

CREATE UNIQUE INDEX idx_producers_product_id_producer_name ON producers(product_id, producer_name);