package com.example.seabattleonline.domain.exception;

import java.util.Map;

public record ExceptionBody(
        String message,
        Map<String, String> errors
) {
}
