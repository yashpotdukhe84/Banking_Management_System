package com.sprint.bank.shared.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ApiResponse<T> {

    private LocalDateTime timestamp;
    private int status;
    private String message;
    private T data;

    public ApiResponse(LocalDateTime timestamp,
                      int status,
                      String message) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.data = null;
    }

    public ApiResponse(LocalDateTime timestamp,
                      int status,
                      String message,
                      T data) {
        this.timestamp = timestamp;
        this.status = status;
        this.message = message;
        this.data = data;
    }
}