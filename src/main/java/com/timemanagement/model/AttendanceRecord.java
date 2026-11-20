package com.timemanagement.model;

import com.timemanagement.model.enums.AttendanceStatus;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Represents an employee attendance record supporting standard and overnight shifts.
 * Uses LocalDate and LocalDateTime for accurate multi-day shift duration and overtime calculations.
 */
public class AttendanceRecord {

    private int recordId;
    private int userId;
    private String userName; // For UI presentation
    private LocalDate recordDate;
    private LocalTime clockIn;
    private LocalDate clockOutDate;
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
        this(recordId, userId, recordDate, clockIn, recordDate, clockOut, hoursWorked, overtimeHours, status, notes);
    }

    public AttendanceRecord(int recordId, int userId, LocalDate recordDate, LocalTime clockIn,
                            LocalDate clockOutDate, LocalTime clockOut, double hoursWorked,
                            double overtimeHours, AttendanceStatus status, String notes) {
        this.recordId = recordId;
        this.userId = userId;
        this.recordDate = recordDate;
        this.clockIn = clockIn;
        this.clockOutDate = clockOutDate;
        this.clockOut = clockOut;
        this.hoursWorked = hoursWorked;
        this.overtimeHours = overtimeHours;
        this.status = status;
        this.notes = notes;
    }

    /**
     * Returns the full clock-in timestamp using recordDate and clockIn.
     */
    public LocalDateTime getClockInDateTime() {
        if (recordDate != null && clockIn != null) {
            return LocalDateTime.of(recordDate, clockIn);
        }
        return null;
    }

    /**
     * Sets clock-in date and time together.
     */
    public void setClockInDateTime(LocalDateTime dateTime) {
        if (dateTime != null) {
            this.recordDate = dateTime.toLocalDate();
            this.clockIn = dateTime.toLocalTime().withNano(0);
        }
    }

    /**
     * Returns the full clock-out timestamp using clockOutDate and clockOut.
     */
    public LocalDateTime getClockOutDateTime() {
        if (clockOutDate != null && clockOut != null) {
            return LocalDateTime.of(clockOutDate, clockOut);
        } else if (recordDate != null && clockOut != null) {
            return LocalDateTime.of(recordDate, clockOut);
        }
        return null;
    }

    /**
     * Sets clock-out date and time together and recalculates hours.
     */
    public void setClockOutDateTime(LocalDateTime dateTime) {
        if (dateTime != null) {
            this.clockOutDate = dateTime.toLocalDate();
            this.clockOut = dateTime.toLocalTime().withNano(0);
            computeHours();
        }
    }

    /**
     * Recalculates hours worked and overtime based on punch timestamps.
     * Rejects invalid clock-out timestamps when clock-out is before clock-in.
     */
    public void computeHours() {
        if (clockIn != null && clockOut != null) {
            LocalDateTime start = getClockInDateTime();
            LocalDateTime end = getClockOutDateTime();

            if (start != null && end != null) {
                if (end.isBefore(start)) {
                    throw new IllegalArgumentException(String.format(
                            "Invalid clock-out timestamp: clock-out (%s) cannot be before clock-in (%s).",
                            end, start));
                }

                long minutes = Duration.between(start, end).toMinutes();
                this.hoursWorked = Math.round((minutes / 60.0) * 100.0) / 100.0;

                if (this.hoursWorked > 8.0) {
                    this.overtimeHours = Math.round((this.hoursWorked - 8.0) * 100.0) / 100.0;
                } else {
                    this.overtimeHours = 0.0;
                }

                if (this.hoursWorked < 4.5 && this.hoursWorked > 0.0) {
                    this.status = AttendanceStatus.HALF_DAY;
                } else if (this.hoursWorked >= 8.0) {
                    this.status = AttendanceStatus.PRESENT;
                }
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
        this.clockIn = clockIn != null ? clockIn.withNano(0) : null;
    }

    public LocalDate getClockOutDate() {
        return clockOutDate;
    }

    public void setClockOutDate(LocalDate clockOutDate) {
        this.clockOutDate = clockOutDate;
    }

    public LocalTime getClockOut() {
        return clockOut;
    }

    public void setClockOut(LocalTime clockOut) {
        this.clockOut = clockOut != null ? clockOut.withNano(0) : null;
        if (this.clockOutDate == null && this.recordDate != null) {
            // Default to recordDate if not explicitly provided
            this.clockOutDate = this.recordDate;
        }
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
