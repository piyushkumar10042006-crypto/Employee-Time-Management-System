package com.timemanagement.exception;

/**
 * Thrown when an employee requests more leave days than their remaining balance.
 */
public class InsufficientLeaveException extends TimeManagementException {
    public InsufficientLeaveException(String message) {
        super(message);
    }
}
