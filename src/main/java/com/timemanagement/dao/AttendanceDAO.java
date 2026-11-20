package com.timemanagement.dao;

import com.timemanagement.db.DBConnection;
import com.timemanagement.exception.AttendanceException;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Attendance operations.
 * Enforces database-level mutual exclusion for active shifts and leases isolated
 * pooled connections via try-with-resources.
 */
public class AttendanceDAO {

    /**
     * Finds the current active (unclosed) shift for a user.
     */
    public Optional<AttendanceRecord> findActiveShift(int userId) throws DatabaseOperationException {
        String sql = "SELECT a.*, u.name as user_name FROM attendance a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.user_id = ? AND a.clock_out IS NULL " +
                     "ORDER BY a.record_date DESC, a.clock_in DESC LIMIT 1";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToAttendance(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching active shift: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /**
     * Checks if a user has an open active shift.
     */
    public boolean hasActiveShift(int userId) throws DatabaseOperationException {
        return findActiveShift(userId).isPresent();
    }

    /**
     * Finds today's attendance record or active open shift for UI dashboard display.
     */
    public Optional<AttendanceRecord> findTodayRecord(int userId) throws DatabaseOperationException {
        // Priority 1: Check if an active open shift exists
        Optional<AttendanceRecord> active = findActiveShift(userId);
        if (active.isPresent()) {
            return active;
        }

        // Priority 2: Check today's completed record
        String sql = "SELECT a.*, u.name as user_name FROM attendance a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.user_id = ? AND a.record_date = CURDATE() " +
                     "ORDER BY a.clock_in DESC LIMIT 1";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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

    /**
     * Clocks in an employee with full LocalDateTime, backed by database transaction lock
     * and unique active-shift constraint to prevent concurrent duplicates.
     */
    public AttendanceRecord clockIn(int userId, LocalDateTime clockInDateTime, String notes)
            throws AttendanceException, DatabaseOperationException {

        LocalDate punchDate = clockInDateTime.toLocalDate();
        LocalTime punchTime = clockInDateTime.toLocalTime().withNano(0);

        AttendanceStatus status = punchTime.isAfter(LocalTime.of(9, 30)) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;

        String lockSql = "SELECT record_id FROM attendance WHERE user_id = ? AND clock_out IS NULL FOR UPDATE";
        String insertSql = "INSERT INTO attendance (user_id, record_date, clock_in, status, notes) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                conn.setAutoCommit(false);

                // 1. Transactional check with row-lock to prevent race conditions
                try (PreparedStatement lockStmt = conn.prepareStatement(lockSql)) {
                    lockStmt.setInt(1, userId);
                    try (ResultSet rs = lockStmt.executeQuery()) {
                        if (rs.next()) {
                            conn.rollback();
                            throw new AttendanceException("You already have an active clock-in session. Please clock out before starting a new shift.");
                        }
                    }
                }

                // 2. Perform insert
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setDate(2, Date.valueOf(punchDate));
                    insertStmt.setTime(3, Time.valueOf(punchTime));
                    insertStmt.setString(4, status.name());
                    insertStmt.setString(5, notes);

                    insertStmt.executeUpdate();

                    int generatedId = 0;
                    try (ResultSet rs = insertStmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                        }
                    }

                    conn.commit();

                    AttendanceRecord record = new AttendanceRecord();
                    record.setRecordId(generatedId);
                    record.setUserId(userId);
                    record.setRecordDate(punchDate);
                    record.setClockIn(punchTime);
                    record.setStatus(status);
                    record.setNotes(notes);
                    return record;
                }
            } catch (SQLException ex) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {}

                // Handle MySQL error 1062 for unique constraint uq_active_user_shift
                if (ex.getErrorCode() == 1062) {
                    throw new AttendanceException("Concurrent check-in blocked: You already have an active clock-in session.");
                }
                throw new DatabaseOperationException("Error recording clock-in: " + ex.getMessage(), ex);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit);
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Database connection error: " + ex.getMessage(), ex);
        }
    }

    /**
     * Backward-compatible clockIn overload using LocalTime.
     */
    public AttendanceRecord clockIn(int userId, LocalTime clockInTime, String notes)
            throws AttendanceException, DatabaseOperationException {
        return clockIn(userId, LocalDateTime.of(LocalDate.now(), clockInTime), notes);
    }

    /**
     * Clocks out an employee using full LocalDateTime, accurately computing overnight shifts
     * and rejecting invalid timestamps.
     */
    public boolean clockOut(int recordId, LocalDateTime clockOutDateTime)
            throws AttendanceException, DatabaseOperationException {

        String selectSql = "SELECT record_date, clock_in FROM attendance WHERE record_id = ?";
        String updateSql = "UPDATE attendance SET clock_out_date = ?, clock_out = ?, hours_worked = ?, overtime_hours = ?, status = ? WHERE record_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            LocalDate inDate = null;
            LocalTime inTime = null;

            try (PreparedStatement selStmt = conn.prepareStatement(selectSql)) {
                selStmt.setInt(1, recordId);
                try (ResultSet rs = selStmt.executeQuery()) {
                    if (rs.next()) {
                        inDate = rs.getDate("record_date").toLocalDate();
                        inTime = rs.getTime("clock_in").toLocalTime();
                    }
                }
            }

            if (inDate == null || inTime == null) {
                return false;
            }

            // Construct AttendanceRecord to compute hours with strict timestamp validation
            AttendanceRecord record = new AttendanceRecord();
            record.setRecordDate(inDate);
            record.setClockIn(inTime);

            try {
                record.setClockOutDateTime(clockOutDateTime);
            } catch (IllegalArgumentException ex) {
                throw new AttendanceException(ex.getMessage());
            }

            try (PreparedStatement updStmt = conn.prepareStatement(updateSql)) {
                updStmt.setDate(1, Date.valueOf(record.getClockOutDate()));
                updStmt.setTime(2, Time.valueOf(record.getClockOut()));
                updStmt.setDouble(3, record.getHoursWorked());
                updStmt.setDouble(4, record.getOvertimeHours());
                updStmt.setString(5, record.getStatus().name());
                updStmt.setInt(6, recordId);

                return updStmt.executeUpdate() > 0;
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error recording clock-out: " + ex.getMessage(), ex);
        }
    }

    /**
     * Backward-compatible clockOut overload using LocalTime.
     */
    public boolean clockOut(int recordId, LocalTime clockOutTime)
            throws AttendanceException, DatabaseOperationException {
        return clockOut(recordId, LocalDateTime.of(LocalDate.now(), clockOutTime));
    }

    public List<AttendanceRecord> findByUserId(int userId) throws DatabaseOperationException {
        List<AttendanceRecord> list = new ArrayList<>();
        String sql = "SELECT a.*, u.name as user_name FROM attendance a " +
                     "JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.user_id = ? ORDER BY a.record_date DESC, a.clock_in DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
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

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        LocalDate clockOutDate = rs.getDate("clock_out_date") != null ? rs.getDate("clock_out_date").toLocalDate() : null;
        LocalTime clockOut = rs.getTime("clock_out") != null ? rs.getTime("clock_out").toLocalTime() : null;
        double hoursWorked = rs.getDouble("hours_worked");
        double overtimeHours = rs.getDouble("overtime_hours");
        AttendanceStatus status = AttendanceStatus.valueOf(rs.getString("status"));
        String notes = rs.getString("notes");

        AttendanceRecord rec = new AttendanceRecord(recordId, userId, date, clockIn, clockOutDate, clockOut, hoursWorked, overtimeHours, status, notes);
        rec.setUserName(userName);
        return rec;
    }
}
