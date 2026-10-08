package com.timemanagement.exception;

/**
 * Thrown when a JDBC database access or SQL query error occurs.
 */
public class DatabaseOperationException extends TimeManagementException {
    public DatabaseOperationException(String message) {
        super(message);
    }

    public DatabaseOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
