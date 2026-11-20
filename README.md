# ⚡ Employee Time Management System

[![Java](https://img.shields.io/badge/Java-17%2F21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Connection Pool](https://img.shields.io/badge/HikariCP-5.1.0-orange?style=for-the-badge)](https://github.com/brettwooldridge/HikariCP)
[![Security](https://img.shields.io/badge/Password-BCrypt%20Salted-red?style=for-the-badge)](https://www.mindrot.org/projects/jBCrypt/)
[![UI](https://img.shields.io/badge/Swing-FlatLaf%203.5-007ACC?style=for-the-badge&logo=java&logoColor=white)](https://www.formdev.com/flatlaf/)
[![Tests](https://img.shields.io/badge/JUnit-5%20Jupiter-25A162?style=for-the-badge&logo=junit5&logoColor=white)](#automated-testing-suite)

> **Academic Project — University Review 2 Technical Upgrade**
> An enterprise-grade desktop Java Swing application built with Core Java, HikariCP Connection Pooling, BCrypt Password Security, JDBC Transactions, and Multithreading to track employee attendance, overnight shifts, and leave approvals with role-based access.

---

## 📋 Table of Contents
- [Project Overview](#project-overview)
- [Review 2 Architectural Improvements](#review-2-architectural-improvements)
- [Core Features & Shift Workflows](#core-features--shift-workflows)
- [Database Schema & Migration Strategy](#database-schema--migration-strategy)
- [Connection Pooling & Transaction Design](#connection-pooling--transaction-design)
- [Password Security & BCrypt Migration](#password-security--bcrypt-migration)
- [Automated Testing Suite (JUnit 5)](#automated-testing-suite)
- [How to Run (macOS & Cross-Platform)](#how-to-run)
- [Demo Credentials](#demo-credentials)
- [Review 2 Presentation Guide](#review-2-presentation-guide)

---

## 🎯 Project Overview

The **Employee Time Management System** eliminates manual logbooks by providing a secure desktop interface for:
1. Recording real-time employee check-in and check-out timestamps with active shift detection.
2. Accurately tracking standard and **overnight shifts crossing midnight** using Gregorian `LocalDateTime`.
3. Preventing concurrent duplicate clock-ins at the **database constraint and transaction level**.
4. Securing credentials using **adaptive BCrypt hashing** with seamless, conditional on-login migration.
5. Managing multi-threaded database access using a **HikariCP Connection Pool** with isolated leases.
6. Enforcing **ACID transaction boundaries** (`TRANSACTION_READ_COMMITTED`) and clean state restoration.
7. Managing leave requests with automated balance deduction and rollback protection on failure.

---

## 🚀 Review 2 Architectural Improvements

Following Review 1 faculty feedback (scored 30 marks), the system underwent rigorous architectural hardening:

| Faculty Review Feedback | Review 1 Status | Review 2 Enterprise Implementation |
| :--- | :--- | :--- |
| **Database Connection Architecture** | Shared Singleton returning a single physical JDBC `Connection` across all background threads. | **HikariCP 5.1.0 Connection Pool**: Every DAO operation leases its own connection via `try-with-resources`. Never shares connections between threads. |
| **Transaction Isolation & State** | Transactions executed on shared connection; no explicit isolation level set. | **Isolated Transactions**: Set `Connection.TRANSACTION_READ_COMMITTED`, `setAutoCommit(false)`, `commit()`, and `rollback()` with safe `autoCommit` restoration in `finally`. |
| **Password Security** | Plain-text passwords in database (`admin123`, `manager123`, `employee123`). | **BCrypt Salted Hashing**: Adaptive cost factor 12. Constant-time verification. Transparent conditional on-login migration preventing race conditions. |
| **Credential Management** | Hardcoded passwords in properties file. | **12-Factor App Security**: Environment variable overrides (`DB_URL`, `DB_USER`, `DB_PASSWORD`), `.gitignore` protection, and clean `db.properties.example` template. |
| **Overnight Shift Handling** | `Duration.between` on `LocalTime` yielded negative durations clamped to `0.0 hrs`. Clock-out failed the next day. | **`LocalDateTime` + `clock_out_date`**: Persists actual clock-in and clock-out dates. Calculates durations and overtime accurately across midnight. Rejects invalid timestamps. |
| **Duplicate Clock-In Prevention** | Checked calendar date only; threw raw SQL error 1062 due to restrictive `uq_user_date`. | **Database-Level Mutual Exclusion**: Virtual column `active_shift_marker` with `UNIQUE KEY uq_active_user_shift` physically guarantees at most ONE open shift per employee. Dropped restrictive `uq_user_date`. |
| **Automated Test Coverage** | Manual `main` runner scripts only. | **JUnit 5 Suite (15 Automated Tests)**: Standardized `mvn test` running against isolated `employee_time_test_db`. Zero modifications to demo/production data. |

---

## 🏗 System Architecture Diagram

```
+---------------------------------------------------------------------------------+
|                               PRESENTATION LAYER                                |
|        LoginFrame   |   MainDashboardFrame   |   UITheme   |   StatusBadges         |
+---------------------------------------------------------------------------------+
                                       │
                        (Dispatches via EDT & SwingWorker)
                                       ▼
+---------------------------------------------------------------------------------+
|                                SERVICE LAYER                                    |
|   AuthService (BCrypt Migration)   |   AttendanceService (LocalDateTime Shifts) |
|   LeaveService (ACID Approvals)    |   ReportService (Java Streams & Analytics) |
+---------------------------------------------------------------------------------+
           │                                                       │
   (Multithreading)                                        (Leases Connection)
           ▼                                                       ▼
+-----------------------+                         +-------------------------------+
|   BACKGROUND WORKERS  |                         |        DAO REPOSITORY         |
|  LiveClockThread      |                         |  UserDAO   |  AttendanceDAO   |
|  AsyncDataWorker<T>   |                         |  LeaveDAO  |  GenericDAO<T>   |
+-----------------------+                         +-------------------------------+
                                                                   │
                                                   (try-with-resources Lease)
                                                                   ▼
                                                  +-------------------------------+
                                                  |   HikariCP CONNECTION POOL    |
                                                  |   Max: 10 | MinIdle: 2        |
                                                  |   Timeout: 30s | AutoCommit:On|
                                                  +-------------------------------+
                                                                   │
                                                                   ▼
                                                  +-------------------------------+
                                                  |        MySQL DATABASE         |
                                                  |  employee_time_db (Production)|
                                                  |  employee_time_test_db (Tests)|
                                                  +-------------------------------+
```

---

## 🗄 Database Schema & Migration Strategy

### 1. `users` Table
* `user_id` (INT, Primary Key, Auto Increment)
* `name` (VARCHAR(100), NOT NULL)
* `email` (VARCHAR(100), UNIQUE, NOT NULL)
* `password` (VARCHAR(255), NOT NULL — BCrypt Hash `$2a$12$...`)
* `role` (VARCHAR(20), 'EMPLOYEE', 'MANAGER', 'ADMIN')
* `department` (VARCHAR(50), NOT NULL)
* `hourly_rate` (DECIMAL(7, 2), DEFAULT 25.00)
* `leave_balance` (INT, DEFAULT 20)
* `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### 2. `attendance` Table (Review 2 Schema)
* `record_id` (INT, Primary Key, Auto Increment)
* `user_id` (INT, Foreign Key $\rightarrow$ `users.user_id`)
* `record_date` (DATE, NOT NULL — Clock-in date)
* `clock_in` (TIME, NOT NULL — Clock-in time)
* `clock_out_date` (DATE, NULL — Actual clock-out date, supports overnight shifts)
* `clock_out` (TIME, NULL — Actual clock-out time)
* `hours_worked` (DECIMAL(5, 2), DEFAULT 0.00)
* `overtime_hours` (DECIMAL(5, 2), DEFAULT 0.00)
* `status` (VARCHAR(20), 'PRESENT', 'HALF_DAY', 'LATE')
* `notes` (VARCHAR(255), NULL)
* `active_shift_marker` (INT, GENERATED ALWAYS AS `(IF(clock_out IS NULL, user_id, NULL))` VIRTUAL)
* **Constraints & Indexes**:
  * `INDEX idx_attendance_user_id (user_id)`
  * `UNIQUE KEY uq_active_user_shift (active_shift_marker)` $\rightarrow$ **Enforces at most 1 active shift per employee**
  * `INDEX idx_attendance_user_date (user_id, record_date)`

### 3. `leave_requests` Table
* `leave_id` (INT, Primary Key, Auto Increment)
* `user_id` (INT, Foreign Key $\rightarrow$ `users.user_id`)
* `leave_type` (VARCHAR(20), 'SICK', 'CASUAL', 'VACATION')
* `start_date` (DATE, NOT NULL)
* `end_date` (DATE, NOT NULL)
* `days_count` (INT, NOT NULL)
* `reason` (TEXT)
* `status` (VARCHAR(20), 'PENDING', 'APPROVED', 'REJECTED')
* `reviewed_by` (INT, Foreign Key $\rightarrow$ `users.user_id`)
* `applied_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

---

## 🔒 Password Security & BCrypt Migration

1. **Format Validation**: `PasswordUtils.isBCryptHash(str)` verifies standard 60-character BCrypt structure (`$2a$`, `$2b$`, `$2y$`).
2. **Re-Hashing Prevention**: `hashPassword` returns the string unchanged if already hashed, preventing catastrophic double-hashing.
3. **On-Login Conditional Migration**:
   * When an employee logs in, `UserDAO.authenticate()` checks `PasswordUtils.verifyPassword()`.
   * If the stored password is legacy plain text, it validates it and immediately executes a **conditional update**:
     ```sql
     UPDATE users SET password = ? WHERE user_id = ? AND password = ?
     ```
   * If multiple concurrent login requests occur, only one updates the row without conflict.
   * All demo accounts (`admin123`, `manager123`, `employee123`) work out of the box with zero forced password resets.

---

## 🧪 Automated Testing Suite (JUnit 5)

All integration tests execute against the dedicated **`employee_time_test_db`** to guarantee that live demo records in `employee_time_db` are never altered or deleted.

Run all tests via Maven:
```bash
mvn test
```

### Verified Test Results (15/15 Passed):
```
[INFO] Running com.timemanagement.ConcurrentClockInTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  ✓ testConcurrentClockInPrevention (5 concurrent threads: 1 succeeds, 4 blocked by DB constraint)

[INFO] Running com.timemanagement.ConnectionPoolConcurrencyTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
  ✓ testConcurrentConnectionBorrowing (20 concurrent threads borrowing & returning connections)

[INFO] Running com.timemanagement.TransactionRollbackTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
  ✓ testTransactionCommitDeductsBalance (atomic leave balance deduction)
  ✓ testTransactionRollbackOnInvalidLeave (failure rolls back without balance deduction)

[INFO] Running com.timemanagement.OvernightShiftTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
  ✓ testStandardDayShift (09:00 to 17:30 = 8.5h worked, 0.5h overtime)
  ✓ testOvernightShiftCrossingMidnight (22:00 to 06:00 next day = 8.0h)
  ✓ testOvernightShiftWithOvertime (20:00 to 06:30 next day = 10.5h worked, 2.5h overtime)
  ✓ testHalfDayShift (4.0h = HALF_DAY)
  ✓ testInvalidClockOutThrowsException (Clock-out before clock-in throws IllegalArgumentException)

[INFO] Running com.timemanagement.PasswordSecurityTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
  ✓ testBCryptFormat (validates 60-character hash structure)
  ✓ testSaltUniqueness (verifies unique salt per invocation)
  ✓ testPasswordVerification (validates correct/incorrect credentials)
  ✓ testAvoidDoubleHashing (idempotent hash generation)
  ✓ testLegacyPlainTextFallback (legacy authentication support)
  ✓ testOnLoginMigration (conditionally upgrades plain-text to BCrypt in DB)

[INFO] Results: Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 🚀 How to Run

### Prerequisites
* **JDK 17 or higher** (Tested on OpenJDK 27 on Apple Silicon M1 macOS)
* **MySQL Server** running locally on port `3306`
* **Maven 3.8+**

### Step 1: Ensure MySQL is Running
```bash
# macOS (Homebrew)
brew services start mysql
```

### Step 2: Configure Environment (Optional)
By default, the application connects to `localhost:3306/employee_time_db` with `root` and empty password.
You can override credentials using environment variables:
```bash
export DB_URL="jdbc:mysql://localhost:3306/employee_time_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
export DB_USER="root"
export DB_PASSWORD=""
```
Or copy the template:
```bash
cp src/main/resources/db.properties.example src/main/resources/db.properties
```

### Step 3: Run the Application
```bash
# Using Maven
mvn clean compile exec:java

# Or using startup shell script
./run.sh
```

---

## 🔑 Demo Credentials

The modern login screen includes **1-Click Autofill Buttons** for instant evaluation:

| Role | Email | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@company.com` | `admin123` | Full system access, team logs, manage users, approve/reject leaves |
| **Manager** | `manager@company.com` | `manager123` | Punch in/out, view team attendance, approve/reject leave requests |
| **Employee** | `employee@company.com` | `employee123` | Punch in/out, view personal history, apply for leave, monthly report export |

---

## 📊 Review 2 Presentation Guide

When presenting for Review 2, highlight the following technical improvements:

1. **Slide 1: Problem Statement & Review 1 Feedback**:
   * Review 1 used a shared Singleton connection resulting in multi-threading concurrency risks.
   * Passwords were stored in plain text.
   * Overnight shifts crossing midnight produced negative durations clamped to zero.
2. **Slide 2: Connection Pooling Architecture (HikariCP)**:
   * Explain HikariCP connection pool configuration (`maxPoolSize=10`, `minIdle=2`, `timeout=30s`).
   * Show how all DAOs borrow isolated connections via `try-with-resources`.
3. **Slide 3: Transaction Isolation & ACID Integrity**:
   * Explain `TRANSACTION_READ_COMMITTED` isolation in `LeaveDAO.updateStatus`.
   * Demonstrate atomic commit on approval and rollback on failure.
4. **Slide 4: Password Security & Seamless Migration**:
   * Explain BCrypt adaptive salt and cost factor 12.
   * Demonstrate conditional on-login migration (`UPDATE users SET password = ? WHERE user_id = ? AND password = ?`).
5. **Slide 5: Overnight Shift Handling**:
   * Show transition to `LocalDateTime` and `clock_out_date`.
   * Demonstrate 22:00 to 06:00 calculation (8.0 hours worked).
   * Show how invalid clock-out timestamps are strictly rejected.
6. **Slide 6: Database-Level Duplicate Shift Prevention**:
   * Present the virtual generated column `active_shift_marker` with `UNIQUE KEY uq_active_user_shift`.
   * Show how concurrent clock-ins are blocked by MySQL itself.
7. **Slide 7: Automated Testing & Verification**:
   * Present JUnit 5 test report (15/15 tests passing).
   * Explain test isolation in `employee_time_test_db`.
