package com.learn.summer.exception;

public class ErrorResponseException extends NestedRuntimeException {

    public final int statusCode;

    public ErrorResponseException(int statusCode) {
        this.statusCode = statusCode;
    }

    public ErrorResponseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public ErrorResponseException(Throwable cause, int statusCode) {
        super(cause);
        this.statusCode = statusCode;
    }

    public ErrorResponseException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }
}
