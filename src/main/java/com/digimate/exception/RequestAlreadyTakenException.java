package com.digimate.exception;

public class RequestAlreadyTakenException extends RuntimeException {

    public RequestAlreadyTakenException(String message) {
        super(message);
    }
}