package com.timemanagement.exception;

/**
 * Thrown when an invalid attendance operation occurs (e.g. double punch-in, punch-out before punch-in).
 */
public class AttendanceException extends TimeManagementException {
    public AttendanceException(String message) {
        super(message);
    }
}
