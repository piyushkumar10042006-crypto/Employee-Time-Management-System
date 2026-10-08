package com.timemanagement.model;

import com.timemanagement.model.enums.LeaveStatus;
import com.timemanagement.model.enums.LeaveType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents an employee leave / time-off request.
 */
public class LeaveRequest {

    private int leaveId;
    private int userId;
    private String userName; // For manager review display
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private int daysCount;
    private String reason;
    private LeaveStatus status;
    private Integer reviewedBy;
    private LocalDateTime appliedAt;

    public LeaveRequest() {
        this.status = LeaveStatus.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    public LeaveRequest(int leaveId, int userId, LeaveType leaveType, LocalDate startDate, 
                        LocalDate endDate, int daysCount, String reason, LeaveStatus status, 
                        Integer reviewedBy, LocalDateTime appliedAt) {
        this.leaveId = leaveId;
        this.userId = userId;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.daysCount = daysCount;
        this.reason = reason;
        this.status = status;
        this.reviewedBy = reviewedBy;
        this.appliedAt = appliedAt;
    }

    // Getters and Setters
    public int getLeaveId() {
        return leaveId;
    }

    public void setLeaveId(int leaveId) {
        this.leaveId = leaveId;
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

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(LeaveType leaveType) {
        this.leaveType = leaveType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getDaysCount() {
        return daysCount;
    }

    public void setDaysCount(int daysCount) {
        this.daysCount = daysCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LeaveStatus getStatus() {
        return status;
    }

    public void setStatus(LeaveStatus status) {
        this.status = status;
    }

    public Integer getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Integer reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
