-- =========================================================
-- Employee Time Management System - Sample Seed Data (Review 2)
-- Uses BCrypt hashed passwords and supports clock_out_date
-- =========================================================

USE employee_time_db;

-- Insert Initial Users with BCrypt hashes (Cost factor 12)
-- admin@company.com    -> admin123
-- manager@company.com  -> manager123
-- employee@company.com -> employee123
-- david@company.com    -> david123
-- emily@company.com    -> emily123
INSERT INTO users (name, email, password, role, department, hourly_rate, leave_balance) VALUES
('System Administrator', 'admin@company.com', '$2a$12$L7RZZbH3v150L6VqC30rquC161j/1Hj49b0k3/XhN95P0E1jP1b3K', 'ADMIN', 'Management', 45.00, 25),
('Sarah Jenkins (Manager)', 'manager@company.com', '$2a$12$Zq8Kk7G0z7t4J8mN9k1lfeK583n/2Jl50c1l4/YiO06Q1F2kQ2c4L', 'MANAGER', 'Engineering', 40.00, 22),
('Alex Morgan', 'employee@company.com', '$2a$12$Wv9Ll8H1a8u5K9nO0l2mgfL694o/3Km61d2m5/ZjP17R2G3lR3d5M', 'EMPLOYEE', 'Engineering', 30.00, 18),
('David Chen', 'david@company.com', '$2a$12$Xw0Mm9I2b9v6L0oP1m3nhgM705p/4Ln72e3n6/AkQ28S3H4mS4e6N', 'EMPLOYEE', 'Engineering', 28.00, 20),
('Emily Watson', 'emily@company.com', '$2a$12$Yx1Nn0J3c0w7M1pQ2n4oihN816q/5Mo83f4o7/BlR39T4I5nT5f7O', 'EMPLOYEE', 'Design', 29.00, 19)
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Insert Sample Attendance Records with clock_out_date
INSERT INTO attendance (user_id, record_date, clock_in, clock_out_date, clock_out, hours_worked, overtime_hours, status, notes) VALUES
(3, CURDATE() - INTERVAL 2 DAY, '09:00:00', CURDATE() - INTERVAL 2 DAY, '17:30:00', 8.50, 0.50, 'PRESENT', 'Sprint planning & feature implementation'),
(3, CURDATE() - INTERVAL 1 DAY, '09:15:00', CURDATE() - INTERVAL 1 DAY, '17:45:00', 8.50, 0.50, 'PRESENT', 'Code reviews & bug fixes'),
(4, CURDATE() - INTERVAL 1 DAY, '09:30:00', CURDATE() - INTERVAL 1 DAY, '17:30:00', 8.00, 0.00, 'LATE', 'Traffic delay'),
(5, CURDATE() - INTERVAL 1 DAY, '09:00:00', CURDATE() - INTERVAL 1 DAY, '13:00:00', 4.00, 0.00, 'HALF_DAY', 'Half-day medical appointment')
ON DUPLICATE KEY UPDATE notes=VALUES(notes);

-- Insert Sample Leave Requests
INSERT INTO leave_requests (user_id, leave_type, start_date, end_date, days_count, reason, status, reviewed_by) VALUES
(3, 'CASUAL', CURDATE() + INTERVAL 5 DAY, CURDATE() + INTERVAL 6 DAY, 2, 'Family wedding event', 'PENDING', NULL),
(4, 'SICK', CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 4 DAY, 2, 'Flu and recovery', 'APPROVED', 2),
(5, 'VACATION', CURDATE() + INTERVAL 12 DAY, CURDATE() + INTERVAL 15 DAY, 4, 'Annual summer trip', 'PENDING', NULL)
ON DUPLICATE KEY UPDATE reason=VALUES(reason);
