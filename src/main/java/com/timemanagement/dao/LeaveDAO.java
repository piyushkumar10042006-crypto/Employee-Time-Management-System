package com.timemanagement.dao;

import com.timemanagement.db.DBConnection;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.LeaveRequest;
import com.timemanagement.model.enums.LeaveStatus;
import com.timemanagement.model.enums.LeaveType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Leave Requests using robust JDBC transactions.
 * Manages atomic leave approval and balance deduction with TRANSACTION_READ_COMMITTED
 * isolation and clean try-with-resources connection leasing.
 */
public class LeaveDAO {

    public LeaveRequest applyLeave(LeaveRequest req) throws DatabaseOperationException {
        String sql = "INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, days_count, reason, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, req.getUserId());
            stmt.setString(2, req.getLeaveType().name());
            stmt.setDate(3, Date.valueOf(req.getStartDate()));
            stmt.setDate(4, Date.valueOf(req.getEndDate()));
            stmt.setInt(5, req.getDaysCount());
            stmt.setString(6, req.getReason());
            stmt.setString(7, req.getStatus().name());

            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    req.setLeaveId(rs.getInt(1));
                }
            }
            return req;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error applying for leave: " + ex.getMessage(), ex);
        }
    }

    public List<LeaveRequest> findByUserId(int userId) throws DatabaseOperationException {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT l.*, u.name as user_name FROM leave_requests l " +
                     "JOIN users u ON l.user_id = u.user_id " +
                     "WHERE l.user_id = ? ORDER BY l.applied_at DESC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToLeave(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching user leaves: " + ex.getMessage(), ex);
        }
        return list;
    }

    public List<LeaveRequest> findAllPending() throws DatabaseOperationException {
        List<LeaveRequest> list = new ArrayList<>();
        String sql = "SELECT l.*, u.name as user_name FROM leave_requests l " +
                     "JOIN users u ON l.user_id = u.user_id " +
                     "WHERE l.status = 'PENDING' ORDER BY l.applied_at ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRowToLeave(rs));
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching pending leaves: " + ex.getMessage(), ex);
        }
        return list;
    }

    /**
     * Updates leave status. If APPROVED, utilizes an atomic JDBC Transaction
     * with TRANSACTION_READ_COMMITTED isolation to safely deduct days from the employee's
     * leave balance. Automatically rolls back on failure and safely restores connection state.
     */
    public boolean updateStatus(int leaveId, LeaveStatus newStatus, int reviewerId) throws DatabaseOperationException {
        String selectSql = "SELECT user_id, days_count FROM leave_requests WHERE leave_id = ? FOR UPDATE";
        String updateLeaveSql = "UPDATE leave_requests SET status = ?, reviewed_by = ? WHERE leave_id = ?";
        String deductSql = "UPDATE users SET leave_balance = GREATEST(0, leave_balance - ?) WHERE user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            try {
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                conn.setAutoCommit(false); // Begin SQL Transaction

                // 1. Fetch leave record details with row lock
                int userId = 0;
                int days = 0;
                try (PreparedStatement selStmt = conn.prepareStatement(selectSql)) {
                    selStmt.setInt(1, leaveId);
                    try (ResultSet rs = selStmt.executeQuery()) {
                        if (rs.next()) {
                            userId = rs.getInt("user_id");
                            days = rs.getInt("days_count");
                        } else {
                            conn.rollback();
                            return false;
                        }
                    }
                }

                // 2. Update leave request table
                try (PreparedStatement updStmt = conn.prepareStatement(updateLeaveSql)) {
                    updStmt.setString(1, newStatus.name());
                    updStmt.setInt(2, reviewerId);
                    updStmt.setInt(3, leaveId);
                    updStmt.executeUpdate();
                }

                // 3. If APPROVED, deduct leave balance from users table
                if (newStatus == LeaveStatus.APPROVED) {
                    try (PreparedStatement deductStmt = conn.prepareStatement(deductSql)) {
                        deductStmt.setInt(1, days);
                        deductStmt.setInt(2, userId);
                        deductStmt.executeUpdate();
                    }
                }

                conn.commit(); // Commit Transaction
                return true;
            } catch (SQLException ex) {
                try {
                    conn.rollback(); // Rollback on error
                } catch (SQLException ignored) {}
                throw new DatabaseOperationException("Transaction failed while updating leave status: " + ex.getMessage(), ex);
            } finally {
                try {
                    conn.setAutoCommit(originalAutoCommit); // Restore auto-commit state
                } catch (SQLException ignored) {}
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Database connection error: " + ex.getMessage(), ex);
        }
    }

    private LeaveRequest mapRowToLeave(ResultSet rs) throws SQLException {
        int leaveId = rs.getInt("leave_id");
        int userId = rs.getInt("user_id");
        String userName = rs.getString("user_name");
        LeaveType type = LeaveType.valueOf(rs.getString("leave_type"));
        java.time.LocalDate start = rs.getDate("start_date").toLocalDate();
        java.time.LocalDate end = rs.getDate("end_date").toLocalDate();
        int days = rs.getInt("days_count");
        String reason = rs.getString("reason");
        LeaveStatus status = LeaveStatus.valueOf(rs.getString("status"));
        Integer reviewer = rs.getObject("reviewed_by") != null ? rs.getInt("reviewed_by") : null;
        Timestamp ts = rs.getTimestamp("applied_at");
        LocalDateTime appliedAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        LeaveRequest req = new LeaveRequest(leaveId, userId, type, start, end, days, reason, status, reviewer, appliedAt);
        req.setUserName(userName);
        return req;
    }
}
