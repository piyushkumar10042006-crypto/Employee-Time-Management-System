package com.timemanagement.service;

import com.timemanagement.dao.LeaveDAO;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.exception.InsufficientLeaveException;
import com.timemanagement.exception.TimeManagementException;
import com.timemanagement.model.LeaveRequest;
import com.timemanagement.model.User;
import com.timemanagement.model.enums.LeaveStatus;
import com.timemanagement.model.enums.LeaveType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service managing leave applications, balance rules, and manager approvals.
 */
public class LeaveService {

    private final LeaveDAO leaveDAO;

    public LeaveService() {
        this.leaveDAO = new LeaveDAO();
    }

    public LeaveService(LeaveDAO leaveDAO) {
        this.leaveDAO = leaveDAO;
    }

    public LeaveRequest applyLeave(User user, LeaveType type, LocalDate startDate, LocalDate endDate, String reason) 
            throws TimeManagementException {
        
        if (user == null) {
            throw new TimeManagementException("User session is required to apply for leave.");
        }
        if (startDate.isAfter(endDate)) {
            throw new TimeManagementException("Start date cannot be after end date.");
        }
        if (startDate.isBefore(LocalDate.now())) {
            throw new TimeManagementException("Leave start date cannot be in the past.");
        }

        int requestedDays = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        if (requestedDays > user.getLeaveBalance()) {
            throw new InsufficientLeaveException(String.format(
                    "Insufficient leave balance! You have %d day(s) available, but requested %d day(s).",
                    user.getLeaveBalance(), requestedDays));
        }

        LeaveRequest request = new LeaveRequest();
        request.setUserId(user.getUserId());
        request.setLeaveType(type);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setDaysCount(requestedDays);
        request.setReason(reason != null ? reason.trim() : "");
        request.setStatus(LeaveStatus.PENDING);

        return leaveDAO.applyLeave(request);
    }

    public List<LeaveRequest> getUserLeaves(int userId) throws DatabaseOperationException {
        return leaveDAO.findByUserId(userId);
    }

    public List<LeaveRequest> getPendingLeaves() throws DatabaseOperationException {
        return leaveDAO.findAllPending();
    }

    public boolean reviewLeave(int leaveId, LeaveStatus status, User reviewer) throws TimeManagementException {
        if (reviewer == null || !reviewer.canApproveLeaves()) {
            throw new TimeManagementException("Unauthorized: Only Managers and Admins can approve or reject leaves.");
        }

        return leaveDAO.updateStatus(leaveId, status, reviewer.getUserId());
    }
}
