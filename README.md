# ⚡ Employee Time Management System

[![Java](https://img.shields.io/badge/Java-17%2F21%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Database](https://img.shields.io/badge/MySQL-8.0%2B-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![UI](https://img.shields.io/badge/Swing-FlatLaf%203.5-007ACC?style=for-the-badge&logo=java&logoColor=white)](https://www.formdev.com/flatlaf/)
[![Architecture](https://img.shields.io/badge/Architecture-DAO%20%7C%20MVC%20%7C%20OOP-success?style=for-the-badge)](#architecture-diagram)

> **Academic Project — Review 1 Submission**  
> A desktop-based Java GUI application built with Core Java, JDBC, and Multithreading to track employee attendance, working hours, and leave workflows with role-based access.

---

## 📋 Table of Contents
- [Project Overview](#project-overview)
- [Review 1 Marking Rubric Compliance](#review-1-marking-rubric-compliance)
- [Core Features & Workflow](#core-features--workflow)
- [Architecture & OOP Concepts](#architecture--oop-concepts)
- [Database Schema & ER Design](#database-schema--er-design)
- [Project Structure](#project-structure)
- [How to Run (macOS M1 & Cross-Platform)](#how-to-run)
- [Demo Credentials](#demo-credentials)
- [Review 1 Presentation Slide Guide](#review-1-presentation-slide-guide)

---

## 🎯 Project Overview

The **Employee Time Management System** eliminates manual logbooks by providing a desktop interface for:
1. Recording real-time employee check-in and check-out timestamps.
2. Automatically calculating daily shift hours and overtime (> 8.0 hours).
3. Managing leave requests with automatic balance validation and manager approval workflows.
4. Providing real-time dashboard tracking via a dedicated background timer thread.
5. Generating summarized time-tracking reports.

---

## 🏆 Review 1 Marking Rubric Compliance

| Rubric Criteria | Marks | Implementation in Codebase | Key Files |
| :--- | :---: | :--- | :--- |
| **OOP Implementation**<br>*(Polymorphism, Inheritance, Exception Handling, Interfaces)* | **10** | • **Inheritance**: `User` (abstract) $\rightarrow$ `Employee`, `Manager`, `Admin`.<br>• **Polymorphism**: Overridden `getRoleBadge()`, `canApproveLeaves()`.<br>• **Interfaces**: `GenericDAO<T, ID>` contract.<br>• **Exception Handling**: Custom hierarchy (`AuthenticationException`, `AttendanceException`, `DatabaseOperationException`, `InsufficientLeaveException`). | `model/User.java`<br>`model/Employee.java`<br>`model/Manager.java`<br>`model/Admin.java`<br>`exception/*` |
| **Collections & Generics** | **6** | • Generics: `GenericDAO<T, ID>` with type safety.<br>• Collections: `List<AttendanceRecord>`, `Map<AttendanceStatus, Long>`.<br>• Streams: Filtering records, calculating total hours, and overtime aggregation. | `dao/GenericDAO.java`<br>`service/ReportService.java` |
| **Multithreading & Synchronization** | **4** | • **Thread 1 (`LiveClockThread`)**: Continuous daemon thread updating digital clock and active shift stopwatch every 1000ms.<br>• **Thread 2 (`AsyncDataWorker`)**: `SwingWorker` for non-blocking asynchronous DB queries.<br>• **Synchronization**: `synchronized` Singleton pattern in `DBConnection` and `SessionManager`. | `thread/LiveClockThread.java`<br>`thread/AsyncDataWorker.java`<br>`service/SessionManager.java`<br>`db/DBConnection.java` |
| **Classes for Database Operations (DAO Pattern)** | **7** | • Full Data Access Object (DAO) architecture separating persistence from business and UI layers.<br>• SQL Transactions (`commit`/`rollback`) when approving leaves and deducting employee balances. | `dao/UserDAO.java`<br>`dao/AttendanceDAO.java`<br>`dao/LeaveDAO.java` |
| **Database Connectivity (JDBC)** | **6** | • Standard JDBC using `DriverManager`, `Connection`, `PreparedStatement`, and `ResultSet`.<br>• Automatic schema and seed table initialization (`DBInitializer`). | `db/DBConnection.java`<br>`db/DBInitializer.java`<br>`resources/db.properties` |

---

## 🏗 Architecture Diagram

```
+-------------------------------------------------------------+
|                     PRESENTATION / GUI                      |
|       LoginFrame  |  MainDashboardFrame  |  UITheme         |
+-------------------------------------------------------------+
                              │
               (Calls Service & Triggers Async)
                              ▼
+-------------------------------------------------------------+
|                       BUSINESS LOGIC                        |
|  AuthService  |  AttendanceService  |  LeaveService         |
|  ReportService (Streams/Collections) |  SessionManager      |
+-------------------------------------------------------------+
          │                                           │
  (Multithreading)                            (Data Queries)
          ▼                                           ▼
+-----------------------+              +----------------------+
|     BACKGROUND        |              |      DAO LAYER       |
|  LiveClockThread      |              |  UserDAO             |
|  AsyncDataWorker      |              |  AttendanceDAO       |
+-----------------------+              |  LeaveDAO            |
                                       +----------------------+
                                                  │
                                          (JDBC Connection)
                                                  ▼
                                       +----------------------+
                                       |  DBConnection (Pool) |
                                       +----------------------+
                                                  │
                                                  ▼
                                       +----------------------+
                                       |   MySQL Database     |
                                       |   (employee_time_db) |
                                       +----------------------+
```

---

## 🗄 Database Schema & ER Design

### 1. `users` Table
Stores staff member credentials, organizational department, and remaining leave days.
* `user_id` (INT, Primary Key, Auto Increment)
* `name` (VARCHAR(100), NOT NULL)
* `email` (VARCHAR(100), UNIQUE, NOT NULL)
* `password` (VARCHAR(255), NOT NULL)
* `role` (VARCHAR(20), 'EMPLOYEE', 'MANAGER', 'ADMIN')
* `department` (VARCHAR(50), NOT NULL)
* `hourly_rate` (DECIMAL(7, 2), DEFAULT 25.00)
* `leave_balance` (INT, DEFAULT 20)
* `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### 2. `attendance` Table
Tracks daily punches and calculated hours.
* `record_id` (INT, Primary Key, Auto Increment)
* `user_id` (INT, Foreign Key $\rightarrow$ `users.user_id`)
* `record_date` (DATE, NOT NULL)
* `clock_in` (TIME, NOT NULL)
* `clock_out` (TIME, NULL)
* `hours_worked` (DECIMAL(5, 2), DEFAULT 0.00)
* `overtime_hours` (DECIMAL(5, 2), DEFAULT 0.00)
* `status` (VARCHAR(20), 'PRESENT', 'HALF_DAY', 'LATE')
* `notes` (VARCHAR(255), NULL)

### 3. `leave_requests` Table
Tracks employee time-off applications and manager reviews.
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

## 🚀 How to Run

### Prerequisites
* **JDK 17 or higher** (Tested on OpenJDK 26 on Apple Silicon M1)
* **MySQL Server** running locally on port `3306`

### Step 1: Ensure MySQL is Running
```bash
# Start MySQL via Homebrew (macOS)
brew services start mysql
```

### Step 2: Configure Database (Optional)
The application automatically creates the database and tables on startup!  
To change credentials, edit `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/employee_time_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true
db.user=root
db.password=
```

### Step 3: Run the Application
You can run using the provided script or Maven:

**Method A: Using Run Script (Quickest on macOS M1)**
```bash
./run.sh
```

**Method B: Using Maven**
```bash
mvn clean compile exec:java
```

---

## 🔑 Demo Credentials

The login screen features **1-Click Autofill Buttons** for instant evaluation:

| Role | Email | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin@company.com` | `admin123` | Full access, team logs, manage users, approve leaves |
| **Manager** | `manager@company.com` | `manager123` | Punch in/out, view team attendance, approve/reject leaves |
| **Employee** | `employee@company.com` | `employee123` | Punch in/out, view personal history, apply for leave, reports |

---

## 📊 Review 1 Presentation Slide Guide

When preparing your Review 1 slides, structure them as follows:

1. **Slide 1: Title & Project Overview**: Project Name, Student Details, and Objective (automating workforce attendance and leave tracking).
2. **Slide 2: System Architecture & Workflow**: Flowchart from User Login $\rightarrow$ Attendance $\rightarrow$ DAO $\rightarrow$ Database.
3. **Slide 3: OOP Implementation**:
   * Inheritance: `User` hierarchy (`Employee`, `Manager`, `Admin`).
   * Polymorphism: Role-based overrides (`canApproveLeaves()`, `getRoleBadge()`).
   * Encapsulation: Private properties with validated accessors.
   * Interfaces: `GenericDAO<T, ID>`.
4. **Slide 4: Database Design & ER Diagram**: Relational diagram displaying `users`, `attendance`, and `leave_requests` tables with Primary and Foreign Keys.
5. **Slide 5: Database Connectivity (JDBC)**:
   * Thread-safe Singleton `DBConnection`.
   * Safe queries with `PreparedStatement`.
   * JDBC Transactions for leave approvals and balance deduction.
6. **Slide 6: Multithreading & Synchronization**:
   * Daemon thread `LiveClockThread` for live UI clock and active shift stopwatch.
   * `AsyncDataWorker` (`SwingWorker`) for non-blocking queries.
7. **Slide 7: UI/UX Design & Demonstration**:
   * Screenshots of Modern Login Card and Main Dashboard with Live Timer.
