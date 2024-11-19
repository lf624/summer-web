package com.learn.summer.exception;

public class ServerErrorException extends ErrorResponseException{
    public ServerErrorException() {
        super(500);
    }

    public ServerErrorException(String message) {
        super(500, message);
    }

    public ServerErrorException(Throwable cause) {
        super(cause, 500);
    }

    public ServerErrorException(String message, Throwable cause) {
        super(message, cause, 500);
    }
}
