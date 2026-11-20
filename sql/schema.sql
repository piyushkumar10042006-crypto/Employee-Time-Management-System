-- =========================================================
-- Employee Time Management System - Database Schema (Review 1)
-- =========================================================

CREATE DATABASE IF NOT EXISTS employee_time_db;
USE employee_time_db;

-- 1. Users Table (Supports Inheritance: Employee, Manager, Admin)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,             -- 'EMPLOYEE', 'MANAGER', 'ADMIN'
    department VARCHAR(50) NOT NULL,
    hourly_rate DECIMAL(7, 2) DEFAULT 25.00,
    leave_balance INT DEFAULT 20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Attendance Table (Records daily punch times & working duration across shifts)
CREATE TABLE IF NOT EXISTS attendance (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    record_date DATE NOT NULL,
    clock_in TIME NOT NULL,
    clock_out_date DATE NULL,
    clock_out TIME NULL,
    hours_worked DECIMAL(5, 2) DEFAULT 0.00,
    overtime_hours DECIMAL(5, 2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT 'PRESENT',   -- 'PRESENT', 'HALF_DAY', 'LATE', 'ON_LEAVE'
    notes VARCHAR(255) NULL,
    active_shift_marker INT GENERATED ALWAYS AS (IF(clock_out IS NULL, user_id, NULL)) VIRTUAL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_attendance_user_id (user_id),
    UNIQUE KEY uq_active_user_shift (active_shift_marker),
    INDEX idx_attendance_user_date (user_id, record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Leave Requests Table (Leave applications & approval status)
CREATE TABLE IF NOT EXISTS leave_requests (
    leave_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    leave_type VARCHAR(20) NOT NULL,       -- 'SICK', 'CASUAL', 'VACATION'
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days_count INT NOT NULL,
    reason TEXT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',  -- 'PENDING', 'APPROVED', 'REJECTED'
    reviewed_by INT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_leave_user (user_id),
    INDEX idx_leave_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
