package org.example.model.dto;

public record EventResponse(String message, EventStatus status, Integer batchSize) {
}
