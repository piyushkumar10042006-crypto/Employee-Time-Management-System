package com.timemanagement.model;

import com.timemanagement.model.enums.AttendanceStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Represents a daily employee attendance record.
 * Encapsulates check-in, check-out, duration calculations, and status.
 */
public class AttendanceRecord {

    private int recordId;
    private int userId;
    private String userName; // Convenient for UI presentation tables
    private LocalDate recordDate;
    private LocalTime clockIn;
    private LocalTime clockOut;
    private double hoursWorked;
    private double overtimeHours;
    private AttendanceStatus status;
    private String notes;

    public AttendanceRecord() {
        this.recordDate = LocalDate.now();
        this.status = AttendanceStatus.PRESENT;
    }

    public AttendanceRecord(int recordId, int userId, LocalDate recordDate, LocalTime clockIn, 
                            LocalTime clockOut, double hoursWorked, double overtimeHours, 
                            AttendanceStatus status, String notes) {
        this.recordId = recordId;
        this.userId = userId;
        this.recordDate = recordDate;
        this.clockIn = clockIn;
        this.clockOut = clockOut;
        this.hoursWorked = hoursWorked;
        this.overtimeHours = overtimeHours;
        this.status = status;
        this.notes = notes;
    }

    /**
     * Recalculates hours worked and overtime based on punch times.
     * Standard work day is 8.0 hours.
     */
    public void computeHours() {
        if (clockIn != null && clockOut != null) {
            long minutes = Duration.between(clockIn, clockOut).toMinutes();
            this.hoursWorked = Math.max(0, Math.round((minutes / 60.0) * 100.0) / 100.0);
            
            if (this.hoursWorked > 8.0) {
                this.overtimeHours = Math.round((this.hoursWorked - 8.0) * 100.0) / 100.0;
            } else {
                this.overtimeHours = 0.0;
            }

            if (this.hoursWorked < 4.5 && this.hoursWorked > 0.0) {
                this.status = AttendanceStatus.HALF_DAY;
            }
        }
    }

    // Getters and Setters
    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public LocalTime getClockIn() {
        return clockIn;
    }

    public void setClockIn(LocalTime clockIn) {
        this.clockIn = clockIn;
    }

    public LocalTime getClockOut() {
        return clockOut;
    }

    public void setClockOut(LocalTime clockOut) {
        this.clockOut = clockOut;
        computeHours();
    }

    public double getHoursWorked() {
        return hoursWorked;
    }

    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
