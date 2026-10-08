package com.timemanagement.dao;

import com.timemanagement.db.DBConnection;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.enums.AttendanceStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Attendance operations.
 * Fulfills Database Operations and Collections criteria.
 */
public class AttendanceDAO {

    public Optional<AttendanceRecord> findTodayRecord(int userId) throws DatabaseOperationException {
        String sql = "SELECT a.*, u.name as user_name FROM attendance a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.user_id = ? AND a.record_date = CURDATE()";
        Connection conn = DBConnection.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAttendance(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching today's attendance: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public AttendanceRecord clockIn(int userId, LocalTime clockInTime, String notes) throws DatabaseOperationException {
        String sql = "INSERT INTO attendance (user_id, record_date, clock_in, status, notes) VALUES (?, CURDATE(), ?, ?, ?)";
        Connection conn = DBConnection.getInstance().getConnection();

        // Check if employee is late (arriving after 09:30)
        AttendanceStatus status = AttendanceStatus.PRESENT;
        if (clockInTime.isAfter(LocalTime.of(9, 30))) {
            status = AttendanceStatus.LATE;
        }

        try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setTime(2, Time.valueOf(clockInTime));
            stmt.setString(3, status.name());
            stmt.setString(4, notes);

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                int generatedId = rs.next() ? rs.getInt(1) : 0;
                AttendanceRecord record = new AttendanceRecord();
                record.setRecordId(generatedId);
                record.setUserId(userId);
                record.setRecordDate(LocalDate.now());
                record.setClockIn(clockInTime);
                record.setStatus(status);
                record.setNotes(notes);
                return record;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error recording clock-in: " + ex.getMessage(), ex);
        }
    }

    public boolean clockOut(int recordId, LocalTime clockOutTime) throws DatabaseOperationException {
        // First retrieve clock_in to compute duration
        String selectSql = "SELECT clock_in FROM attendance WHERE record_id = ?";
        String updateSql = "UPDATE attendance SET clock_out = ?, hours_worked = ?, overtime_hours = ?, status = ? WHERE record_id = ?";
        Connection conn = DBConnection.getInstance().getConnection();

        try {
            LocalTime clockIn = null;
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, recordId);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        clockIn = rs.getTime("clock_in").toLocalTime();
                    }
                }
            }

            if (clockIn == null) {
                return false;
            }

            AttendanceRecord tempRecord = new AttendanceRecord();
            tempRecord.setClockIn(clockIn);
            tempRecord.setClockOut(clockOutTime); // computes hours and overtime

            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setTime(1, Time.valueOf(clockOutTime));
                updateStmt.setDouble(2, tempRecord.getHoursWorked());
                updateStmt.setDouble(3, tempRecord.getOvertimeHours());
                updateStmt.setString(4, tempRecord.getStatus().name());
                updateStmt.setInt(5, recordId);

                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error recording clock-out: " + ex.getMessage(), ex);
        }
    }

    public List<AttendanceRecord> findByUserId(int userId) throws DatabaseOperationException {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name as user_name FROM attendance a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.user_id = ? ORDER BY a.record_date DESC";
        Connection conn = DBConnection.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttendance(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching attendance history: " + ex.getMessage(), ex);
        }
        return list;
    }

    public List<AttendanceRecord> findAllWithUserDetails() throws DatabaseOperationException {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name as user_name FROM attendance a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "ORDER BY a.record_date DESC, a.clock_in DESC";
        Connection conn = DBConnection.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToAttendance(rs));
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching all attendance records: " + ex.getMessage(), ex);
        }
        return list;
    }

    public List<AttendanceRecord> findByDateRange(int userId, LocalDate start, LocalDate end) throws DatabaseOperationException {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name as user_name FROM attendance a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.user_id = ? AND a.record_date BETWEEN ? AND ? " +
                     "ORDER BY a.record_date ASC";
        Connection conn = DBConnection.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(start));
            stmt.setDate(3, Date.valueOf(end));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToAttendance(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching attendance date range: " + ex.getMessage(), ex);
        }
        return list;
    }

    private AttendanceRecord mapRowToAttendance(ResultSet rs) throws SQLException {
        int recordId = rs.getInt("record_id");
        int userId = rs.getInt("user_id");
        String userName = rs.getString("user_name");
        LocalDate date = rs.getDate("record_date").toLocalDate();
        LocalTime clockIn = rs.getTime("clock_in") != null ? rs.getTime("clock_in").toLocalTime() : null;
        LocalTime clockOut = rs.getTime("clock_out") != null ? rs.getTime("clock_out").toLocalTime() : null;
        double hoursWorked = rs.getDouble("hours_worked");
        double overtimeHours = rs.getDouble("overtime_hours");
        AttendanceStatus status = AttendanceStatus.valueOf(rs.getString("status"));
        String notes = rs.getString("notes");

        AttendanceRecord rec = new AttendanceRecord(recordId, userId, date, clockIn, clockOut, hoursWorked, overtimeHours, status, notes);
        rec.setUserName(userName);
        return rec;
    }
}
