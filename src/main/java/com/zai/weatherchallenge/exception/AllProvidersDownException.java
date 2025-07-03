package com.zai.weatherchallenge.exception;

public class AllProvidersDownException extends RuntimeException {
    public AllProvidersDownException(String message, Throwable cause) {
        super(message, cause);
    }
}