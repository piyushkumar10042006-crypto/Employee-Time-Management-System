package com.timemanagement.exception;

/**
 * Base exception for all domain-specific errors in the Employee Time Management System.
 * Demonstrates custom exception handling in OOP.
 */
public class TimeManagementException extends Exception {
    public TimeManagementException(String message) {
        super(message);
    }

    public TimeManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
