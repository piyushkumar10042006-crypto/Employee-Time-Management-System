package com.timemanagement.db;

import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.util.PasswordUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Initializes database schema, applies non-destructive Review 2 migrations,
 * and seeds initial demo data using connection pooling and BCrypt encryption.
 */
public class DBInitializer {

    public static void initializeDatabase() throws DatabaseOperationException {
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // 1. Create Users Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  user_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  name VARCHAR(100) NOT NULL," +
                "  email VARCHAR(100) NOT NULL UNIQUE," +
                "  password VARCHAR(255) NOT NULL," +
                "  role VARCHAR(20) NOT NULL," +
                "  department VARCHAR(50) NOT NULL," +
                "  hourly_rate DECIMAL(7, 2) DEFAULT 25.00," +
                "  leave_balance INT DEFAULT 20," +
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  INDEX idx_user_email (email)," +
                "  INDEX idx_user_role (role)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

            // 2. Create Attendance Table (Review 2 Schema)
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS attendance (" +
                "  record_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  record_date DATE NOT NULL," +
                "  clock_in TIME NOT NULL," +
                "  clock_out_date DATE NULL," +
                "  clock_out TIME NULL," +
                "  hours_worked DECIMAL(5, 2) DEFAULT 0.00," +
                "  overtime_hours DECIMAL(5, 2) DEFAULT 0.00," +
                "  status VARCHAR(20) DEFAULT 'PRESENT'," +
                "  notes VARCHAR(255) NULL," +
                "  active_shift_marker INT GENERATED ALWAYS AS (IF(clock_out IS NULL, user_id, NULL)) VIRTUAL," +
                "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                "  INDEX idx_attendance_user_id (user_id)," +
                "  UNIQUE KEY uq_active_user_shift (active_shift_marker)," +
                "  INDEX idx_attendance_user_date (user_id, record_date)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

            // 3. Create Leave Requests Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS leave_requests (" +
                "  leave_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  leave_type VARCHAR(20) NOT NULL," +
                "  start_date DATE NOT NULL," +
                "  end_date DATE NOT NULL," +
                "  days_count INT NOT NULL," +
                "  reason TEXT NULL," +
                "  status VARCHAR(20) DEFAULT 'PENDING'," +
                "  reviewed_by INT NULL," +
                "  applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                "  FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL," +
                "  INDEX idx_leave_user (user_id)," +
                "  INDEX idx_leave_status (status)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

            // 4. Apply Idempotent Schema Migrations for Existing Databases
            applyMigrationsIfNecessary(conn, stmt);

            // 5. Seed initial demo users with BCrypt hashes if users table is empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String adminPass = PasswordUtils.hashPassword("admin123");
                    String mgrPass = PasswordUtils.hashPassword("manager123");
                    String empPass = PasswordUtils.hashPassword("employee123");
                    String davidPass = PasswordUtils.hashPassword("david123");
                    String emilyPass = PasswordUtils.hashPassword("emily123");

                    String insertSql = String.format(
                        "INSERT INTO users (name, email, password, role, department, hourly_rate, leave_balance) VALUES " +
                        "('System Administrator', 'admin@company.com', '%s', 'ADMIN', 'Management', 45.00, 25)," +
                        "('Sarah Jenkins (Manager)', 'manager@company.com', '%s', 'MANAGER', 'Engineering', 40.00, 22)," +
                        "('Alex Morgan', 'employee@company.com', '%s', 'EMPLOYEE', 'Engineering', 30.00, 18)," +
                        "('David Chen', 'david@company.com', '%s', 'EMPLOYEE', 'Engineering', 28.00, 20)," +
                        "('Emily Watson', 'emily@company.com', '%s', 'EMPLOYEE', 'Design', 29.00, 19);",
                        adminPass, mgrPass, empPass, davidPass, emilyPass
                    );
                    stmt.executeUpdate(insertSql);

                    // Sample attendance record
                    stmt.executeUpdate(
                        "INSERT INTO attendance (user_id, record_date, clock_in, clock_out_date, clock_out, hours_worked, overtime_hours, status, notes) VALUES " +
                        "(3, CURDATE() - INTERVAL 1 DAY, '09:00:00', CURDATE() - INTERVAL 1 DAY, '17:30:00', 8.50, 0.50, 'PRESENT', 'Sprint development work');"
                    );

                    // Sample leave request
                    stmt.executeUpdate(
                        "INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, days_count, reason, status) VALUES " +
                        "(3, 'CASUAL', CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 6 DAY, 2, 'Family function', 'PENDING');"
                    );
                }
            }
        } catch (Exception ex) {
            throw new DatabaseOperationException("Failed to initialize database tables: " + ex.getMessage(), ex);
        }
    }

    /**
     * Inspects metadata and applies non-destructive Review 2 migrations if needed.
     */
    private static void applyMigrationsIfNecessary(Connection conn, Statement stmt) {
        try {
            DatabaseMetaData meta = conn.getMetaData();

            // Check if clock_out_date column exists
            boolean hasClockOutDate = false;
            try (ResultSet rs = meta.getColumns(null, null, "attendance", "clock_out_date")) {
                if (rs.next()) {
                    hasClockOutDate = true;
                }
            }
            if (!hasClockOutDate) {
                stmt.executeUpdate("ALTER TABLE attendance ADD COLUMN clock_out_date DATE NULL AFTER clock_in");
                stmt.executeUpdate("UPDATE attendance SET clock_out_date = record_date WHERE clock_out_date IS NULL AND clock_out IS NOT NULL");
            }

            // Check if idx_attendance_user_id exists before dropping uq_user_date
            boolean hasUserIdIndex = false;
            try (ResultSet rs = meta.getIndexInfo(null, null, "attendance", false, false)) {
                while (rs.next()) {
                    String idxName = rs.getString("INDEX_NAME");
                    if ("idx_attendance_user_id".equalsIgnoreCase(idxName)) {
                        hasUserIdIndex = true;
                        break;
                    }
                }
            }
            if (!hasUserIdIndex) {
                stmt.executeUpdate("ALTER TABLE attendance ADD INDEX idx_attendance_user_id (user_id)");
            }

            // Check if legacy uq_user_date exists
            boolean hasLegacyUqUserDate = false;
            try (ResultSet rs = meta.getIndexInfo(null, null, "attendance", false, false)) {
                while (rs.next()) {
                    String idxName = rs.getString("INDEX_NAME");
                    if ("uq_user_date".equalsIgnoreCase(idxName)) {
                        hasLegacyUqUserDate = true;
                        break;
                    }
                }
            }
            if (hasLegacyUqUserDate) {
                stmt.executeUpdate("ALTER TABLE attendance DROP INDEX uq_user_date");
            }

            // Check if active_shift_marker column exists
            boolean hasActiveShiftMarker = false;
            try (ResultSet rs = meta.getColumns(null, null, "attendance", "active_shift_marker")) {
                if (rs.next()) {
                    hasActiveShiftMarker = true;
                }
            }
            if (!hasActiveShiftMarker) {
                stmt.executeUpdate("ALTER TABLE attendance ADD COLUMN active_shift_marker INT GENERATED ALWAYS AS (IF(clock_out IS NULL, user_id, NULL)) VIRTUAL");
            }

            // Check if uq_active_user_shift index exists
            boolean hasUqActiveShift = false;
            try (ResultSet rs = meta.getIndexInfo(null, null, "attendance", false, false)) {
                while (rs.next()) {
                    String idxName = rs.getString("INDEX_NAME");
                    if ("uq_active_user_shift".equalsIgnoreCase(idxName)) {
                        hasUqActiveShift = true;
                        break;
                    }
                }
            }
            if (!hasUqActiveShift) {
                stmt.executeUpdate("ALTER TABLE attendance ADD UNIQUE KEY uq_active_user_shift (active_shift_marker)");
            }
        } catch (Exception ex) {
            System.err.println("Notice: Migration check completed: " + ex.getMessage());
        }
    }
}
