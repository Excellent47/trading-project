package org.example.model.dto;

public record ErrorResponse(
        String message,
        int status,
        String timestamp,
        String path,
        String errorType
) {
    public ErrorResponse(String message, int status, String path, String errorType) {
        this(message, status, java.time.Instant.now().toString(), path, errorType);
    }
}