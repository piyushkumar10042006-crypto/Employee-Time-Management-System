package com.timemanagement.db;

import com.timemanagement.exception.DatabaseOperationException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Automatically initializes database schema and initial seed data if not present.
 */
public class DBInitializer {

    public static void initializeDatabase() throws DatabaseOperationException {
        Connection conn = DBConnection.getInstance().getConnection();

        try (Statement stmt = conn.createStatement()) {
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
                "  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

            // 2. Create Attendance Table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS attendance (" +
                "  record_id INT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id INT NOT NULL," +
                "  record_date DATE NOT NULL," +
                "  clock_in TIME NOT NULL," +
                "  clock_out TIME NULL," +
                "  hours_worked DECIMAL(5, 2) DEFAULT 0.00," +
                "  overtime_hours DECIMAL(5, 2) DEFAULT 0.00," +
                "  status VARCHAR(20) DEFAULT 'PRESENT'," +
                "  notes VARCHAR(255) NULL," +
                "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE," +
                "  UNIQUE KEY uq_user_date (user_id, record_date)" +
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
                "  FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"
            );

            // 4. Seed default demo data if users table is empty
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(
                    "INSERT INTO users (name, email, password, role, department, hourly_rate, leave_balance) VALUES " +
                    "('System Administrator', 'admin@company.com', 'admin123', 'ADMIN', 'Management', 45.00, 25)," +
                    "('Sarah Jenkins (Manager)', 'manager@company.com', 'manager123', 'MANAGER', 'Engineering', 40.00, 22)," +
                    "('Alex Morgan', 'employee@company.com', 'employee123', 'EMPLOYEE', 'Engineering', 30.00, 18)," +
                    "('David Chen', 'david@company.com', 'david123', 'EMPLOYEE', 'Engineering', 28.00, 20)," +
                    "('Emily Watson', 'emily@company.com', 'emily123', 'EMPLOYEE', 'Design', 29.00, 19);"
                );

                // Sample attendance record
                stmt.executeUpdate(
                    "INSERT INTO attendance (user_id, record_date, clock_in, clock_out, hours_worked, overtime_hours, status, notes) VALUES " +
                    "(3, CURDATE() - INTERVAL 1 DAY, '09:00:00', '17:30:00', 8.50, 0.50, 'PRESENT', 'Sprint development work');"
                );

                // Sample leave request
                stmt.executeUpdate(
                    "INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, days_count, reason, status) VALUES " +
                    "(3, 'CASUAL', CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 6 DAY, 2, 'Family function', 'PENDING');"
                );
            }
        } catch (Exception ex) {
            throw new DatabaseOperationException("Failed to initialize database tables: " + ex.getMessage(), ex);
        }
    }
}
