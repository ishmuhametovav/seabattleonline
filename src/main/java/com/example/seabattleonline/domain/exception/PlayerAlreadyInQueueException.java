package com.example.seabattleonline.domain.exception;

public class PlayerAlreadyInQueueException extends RuntimeException {
    public PlayerAlreadyInQueueException(final String message) {
        super(message);
    }
}
