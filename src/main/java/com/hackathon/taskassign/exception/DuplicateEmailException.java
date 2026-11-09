package com.hackathon.taskassign.exception;

/** Thrown when an employee email is already used by another employee. */
public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("An employee with email '" + email + "' already exists.");
    }
}
