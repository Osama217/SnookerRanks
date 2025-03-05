package com.egr.snookerrank.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class RestApiResponse<T> {
    // Getters and Setters
    private String status; // SUCCESS, FAILURE
    private String message;
    private T data;
    private String errorDetails; // Optional, for errors

    // Constructors
    public RestApiResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }


}