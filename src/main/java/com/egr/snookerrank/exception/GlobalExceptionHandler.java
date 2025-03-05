package com.egr.snookerrank.exception;

import com.egr.snookerrank.dto.RestApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<RestApiResponse<?>> handleRuntimeException(RuntimeException e) {
        RestApiResponse<?> response = new RestApiResponse<>("FAILURE", e.getMessage(), null, e.toString());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestApiResponse<?>> handleException(Exception e) {
        RestApiResponse<?> response = new RestApiResponse<>("FAILURE", "An internal error occurred", null, e.toString());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}