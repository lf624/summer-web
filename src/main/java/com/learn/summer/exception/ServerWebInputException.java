package com.learn.summer.exception;

public class ServerWebInputException extends ErrorResponseException {

    public ServerWebInputException() {
        super(400);
    }

    public ServerWebInputException(String message) {
        super(400, message);
    }

    public ServerWebInputException(Throwable cause) {
        super(cause, 400);
    }

    public ServerWebInputException(String message, Throwable cause) {
        super(message, cause, 400);
    }
}
