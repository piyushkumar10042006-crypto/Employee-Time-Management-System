-- =========================================================
-- Employee Time Management System - Sample Seed Data
-- =========================================================

USE employee_time_db;

-- Insert Initial Users (Passwords stored in plain text for demo; can be hashed)
INSERT INTO users (name, email, password, role, department, hourly_rate, leave_balance) VALUES
('System Administrator', 'admin@company.com', 'admin123', 'ADMIN', 'Management', 45.00, 25),
('Sarah Jenkins (Manager)', 'manager@company.com', 'manager123', 'MANAGER', 'Engineering', 40.00, 22),
('Alex Morgan', 'employee@company.com', 'employee123', 'EMPLOYEE', 'Engineering', 30.00, 18),
('David Chen', 'david@company.com', 'david123', 'EMPLOYEE', 'Engineering', 28.00, 20),
('Emily Watson', 'emily@company.com', 'emily123', 'EMPLOYEE', 'Design', 29.00, 19)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert Sample Attendance Records
INSERT INTO attendance (user_id, record_date, clock_in, clock_out, hours_worked, overtime_hours, status, notes) VALUES
(3, CURDATE() - INTERVAL 2 DAY, '09:00:00', '17:30:00', 8.50, 0.50, 'PRESENT', 'Sprint planning & feature implementation'),
(3, CURDATE() - INTERVAL 1 DAY, '09:15:00', '17:45:00', 8.50, 0.50, 'PRESENT', 'Code reviews & bug fixes'),
(4, CURDATE() - INTERVAL 1 DAY, '09:30:00', '17:30:00', 8.00, 0.00, 'LATE', 'Traffic delay'),
(5, CURDATE() - INTERVAL 1 DAY, '09:00:00', '13:00:00', 4.00, 0.00, 'HALF_DAY', 'Half-day medical appointment')
ON DUPLICATE KEY UPDATE notes=VALUES(notes);

-- Insert Sample Leave Requests
INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, days_count, reason, status, reviewed_by) VALUES
(3, 'CASUAL', CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 6 DAY, 2, 'Family wedding event', 'PENDING', NULL),
(4, 'SICK', CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 4 DAY, 2, 'Flu and recovery', 'APPROVED', 2),
(5, 'VACATION', CURDATE() + INTERVAL 12 DAY, CURDATE() + INTERVAL 15 DAY, 4, 'Annual summer trip', 'PENDING', NULL)
ON DUPLICATE KEY UPDATE reason=VALUES(reason);
