package com.hackathon.taskassign.exception;

/** Base class for expected business-rule failures (shown to the user as a message). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
