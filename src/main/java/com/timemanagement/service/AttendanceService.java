package com.timemanagement.service;

import com.timemanagement.dao.AttendanceDAO;
import com.timemanagement.exception.AttendanceException;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating attendance operations, multi-day shifts, and active shift tracking.
 */
public class AttendanceService {

    private final AttendanceDAO attendanceDAO;

    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAO();
    }

    public AttendanceService(AttendanceDAO attendanceDAO) {
        this.attendanceDAO = attendanceDAO;
    }

    public AttendanceRecord clockIn(User user, String notes) throws AttendanceException, DatabaseOperationException {
        return clockIn(user, LocalDateTime.now(), notes);
    }

    public AttendanceRecord clockIn(User user, LocalDateTime clockInDateTime, String notes)
            throws AttendanceException, DatabaseOperationException {
        if (user == null) {
            throw new AttendanceException("User must be logged in to clock in.");
        }

        Optional<AttendanceRecord> active = attendanceDAO.findActiveShift(user.getUserId());
        if (active.isPresent()) {
            AttendanceRecord rec = active.get();
            throw new AttendanceException(String.format(
                    "You have an active clock-in session from %s at %s. Please clock out before starting a new shift.",
                    rec.getRecordDate(), rec.getClockIn()));
        }

        return attendanceDAO.clockIn(user.getUserId(), clockInDateTime, notes);
    }

    public boolean clockOut(User user) throws AttendanceException, DatabaseOperationException {
        return clockOut(user, LocalDateTime.now());
    }

    public boolean clockOut(User user, LocalDateTime clockOutDateTime) throws AttendanceException, DatabaseOperationException {
        if (user == null) {
            throw new AttendanceException("User must be logged in to clock out.");
        }

        Optional<AttendanceRecord> active = attendanceDAO.findActiveShift(user.getUserId());
        if (active.isEmpty()) {
            throw new AttendanceException("You do not have an active clock-in session. Please clock in first.");
        }

        return attendanceDAO.clockOut(active.get().getRecordId(), clockOutDateTime);
    }

    public Optional<AttendanceRecord> getTodayRecord(int userId) throws DatabaseOperationException {
        return attendanceDAO.findTodayRecord(userId);
    }

    public Optional<AttendanceRecord> getActiveShift(int userId) throws DatabaseOperationException {
        return attendanceDAO.findActiveShift(userId);
    }

    public List<AttendanceRecord> getUserAttendanceHistory(int userId) throws DatabaseOperationException {
        return attendanceDAO.findByUserId(userId);
    }

    public List<AttendanceRecord> getAllAttendanceRecords() throws DatabaseOperationException {
        return attendanceDAO.findAllWithUserDetails();
    }
}
