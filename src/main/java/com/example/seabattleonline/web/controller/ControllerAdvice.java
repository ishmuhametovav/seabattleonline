package com.example.seabattleonline.web.controller;

import com.example.seabattleonline.domain.exception.AccessDeniedException;
import com.example.seabattleonline.domain.exception.ConnectionLostException;
import com.example.seabattleonline.domain.exception.ExceptionBody;
import com.example.seabattleonline.domain.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ExceptionBody handleAccessDeniedException(final AccessDeniedException e) {
        e.printStackTrace();
        return new ExceptionBody(e.getMessage(), null);
    }

    @ExceptionHandler(ConnectionLostException.class)
    @ResponseStatus(HttpStatus.GATEWAY_TIMEOUT)
    ExceptionBody handleConnectionLostException(
            final ConnectionLostException e
    ) {
        e.printStackTrace();
        return new ExceptionBody(e.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ExceptionBody handleResourceNotFoundException(
            final ResourceNotFoundException e
    ) {
        e.printStackTrace();
        return new ExceptionBody(e.getMessage(), null);
    }
}
