package com.timemanagement.exception;

/**
 * Thrown when user authentication fails or credentials are invalid.
 */
public class AuthenticationException extends TimeManagementException {
    public AuthenticationException(String message) {
        super(message);
    }
}
