package com.hackathon.taskassign.exception;

/** Thrown when an employee or task with the requested id does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
