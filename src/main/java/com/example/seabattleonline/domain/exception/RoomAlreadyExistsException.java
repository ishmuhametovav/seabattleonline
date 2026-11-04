package com.example.seabattleonline.domain.exception;

public class RoomAlreadyExistsException extends RuntimeException {
    public RoomAlreadyExistsException(final String message) {
        super(message);
    }
}
