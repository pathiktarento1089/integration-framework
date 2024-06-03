package com.tarento.integration.integrationframework.exception;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Setter
@Getter
public class CustomException extends RuntimeException {

    private String code;
    private String message;
    private String httpStatusCode;
    private Map<String, String> errors;

    public CustomException() {
    }

    public CustomException(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public CustomException(String code, String message, String httpStatusCode) {
        this.code = code;
        this.message = message;
        this.httpStatusCode = httpStatusCode;
    }

    public CustomException(Map<String, String> errors) {
        this.message = errors.toString();
        this.errors = errors;
    }
}
