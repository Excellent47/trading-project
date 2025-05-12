package org.example.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static java.time.LocalDateTime.now;

/**
 * Для упрощения, данный класс используется и как Entity и как DTO.
 * Такое использование служит только для демонстрации.
 */
public record Producer(
        @JsonProperty("id") UUID id,
        @JsonProperty("product_id") long productId,
        @JsonProperty("producer_name") String producerName,
        @JsonProperty("price") int price,
        @JsonProperty("created") LocalDateTime created,
        @JsonProperty("updated") LocalDateTime updated) {

    public Producer {
        if (productId < 1) {
            throw new RuntimeException("Некорректный productId. Значение должно быть положительным");
        }
        if (price < 1) {
            throw new RuntimeException("Некорректный price. Значение должно быть положительным");
        }
        if (producerName == null || producerName.isBlank()) {
            throw new RuntimeException("Некорректный producerName. Значение должно быть заполненным.");
        }
        id = UUID.randomUUID();
        created = now();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Producer producer = (Producer) o;
        return Objects.equals(productId, producer.productId) && Objects.equals(producerName, producer.producerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, producerName);
    }
}
