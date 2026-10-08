package com.timemanagement.service;

import com.timemanagement.dao.AttendanceDAO;
import com.timemanagement.exception.AttendanceException;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.User;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating attendance operations and enforcing time-tracking business logic.
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
        if (user == null) {
            throw new AttendanceException("User must be logged in to clock in.");
        }

        Optional<AttendanceRecord> existing = attendanceDAO.findTodayRecord(user.getUserId());
        if (existing.isPresent()) {
            throw new AttendanceException("You have already clocked in today at " + existing.get().getClockIn());
        }

        return attendanceDAO.clockIn(user.getUserId(), LocalTime.now(), notes);
    }

    public boolean clockOut(User user) throws AttendanceException, DatabaseOperationException {
        if (user == null) {
            throw new AttendanceException("User must be logged in to clock out.");
        }

        Optional<AttendanceRecord> existing = attendanceDAO.findTodayRecord(user.getUserId());
        if (existing.isEmpty()) {
            throw new AttendanceException("You cannot clock out without clocking in first today.");
        }

        AttendanceRecord record = existing.get();
        if (record.getClockOut() != null) {
            throw new AttendanceException("You have already clocked out today at " + record.getClockOut());
        }

        return attendanceDAO.clockOut(record.getRecordId(), LocalTime.now());
    }

    public Optional<AttendanceRecord> getTodayRecord(int userId) throws DatabaseOperationException {
        return attendanceDAO.findTodayRecord(userId);
    }

    public List<AttendanceRecord> getUserAttendanceHistory(int userId) throws DatabaseOperationException {
        return attendanceDAO.findByUserId(userId);
    }

    public List<AttendanceRecord> getAllAttendanceRecords() throws DatabaseOperationException {
        return attendanceDAO.findAllWithUserDetails();
    }
}
