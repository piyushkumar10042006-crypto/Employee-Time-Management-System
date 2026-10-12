package com.timemanagement;

import com.timemanagement.db.DBConnection;
import com.timemanagement.db.DBInitializer;
import com.timemanagement.exception.AttendanceException;
import com.timemanagement.exception.AuthenticationException;
import com.timemanagement.model.Admin;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.Employee;
import com.timemanagement.model.LeaveRequest;
import com.timemanagement.model.Manager;
import com.timemanagement.model.User;
import com.timemanagement.model.enums.LeaveStatus;
import com.timemanagement.model.enums.LeaveType;
import com.timemanagement.service.AttendanceService;
import com.timemanagement.service.AuthService;
import com.timemanagement.service.LeaveService;
import com.timemanagement.service.ReportService;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * End-to-end Integration Test verifying Live Database Operations,
 * Authentication, Punch In/Out, Leave Workflow, and Report Export.
 */
public class LiveDatabaseIntegrationTest {

    private static int passed = 0;
    private static int total = 0;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("   Live MySQL Database & Backend Integration Audit Test      ");
        System.out.println("=============================================================");

        run("1. Database Connection & Schema Initialization", LiveDatabaseIntegrationTest::testDatabaseInit);
        run("2. Employee Authentication (Valid & Invalid)", LiveDatabaseIntegrationTest::testEmployeeAuth);
        run("3. Manager Authentication & Permissions", LiveDatabaseIntegrationTest::testManagerAuth);
        run("4. Attendance Punch-In, Punch-Out & Validation", LiveDatabaseIntegrationTest::testAttendanceWorkflow);
        run("5. Leave Request Submission & Manager Approval", LiveDatabaseIntegrationTest::testLeaveWorkflow);
        run("6. Reports Calculation & TXT File Export", LiveDatabaseIntegrationTest::testReportAndExport);

        System.out.println("=============================================================");
        System.out.printf("Integration Audit: %d / %d Tests Passed.\n", passed, total);
        System.out.println("=============================================================");

        if (passed != total) {
            System.exit(1);
        }
    }

    private static void run(String name, Runnable test) {
        total++;
        try {
            test.run();
            System.out.printf("  ✓ [PASS] %s\n", name);
            passed++;
        } catch (Throwable t) {
            System.out.printf("  ❌ [FAIL] %s: %s\n", name, t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("Failed: " + msg);
    }

    private static void testDatabaseInit() {
        try {
            DBInitializer.initializeDatabase();
            Connection conn = DBConnection.getInstance().getConnection();
            assertTrue(conn != null && !conn.isClosed(), "Active MySQL Connection must be open");
        } catch (Exception e) {
            throw new RuntimeException("Database init failed: " + e.getMessage(), e);
        }
    }

    private static void testEmployeeAuth() {
        try {
            AuthService auth = new AuthService();
            User emp = auth.login("employee@company.com", "employee123");
            assertTrue(emp instanceof Employee, "Authenticated user must be an Employee");
            assertTrue(!emp.canApproveLeaves(), "Employee cannot approve leaves");

            // Test bad credentials
            boolean caughtBadAuth = false;
            try {
                auth.login("employee@company.com", "wrongpassword");
            } catch (AuthenticationException ex) {
                caughtBadAuth = true;
            }
            assertTrue(caughtBadAuth, "Wrong password must throw AuthenticationException");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testManagerAuth() {
        try {
            AuthService auth = new AuthService();
            User mgr = auth.login("manager@company.com", "manager123");
            assertTrue(mgr instanceof Manager, "Authenticated user must be a Manager");
            assertTrue(mgr.canApproveLeaves(), "Manager must have leave approval permissions");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testAttendanceWorkflow() {
        try {
            AuthService auth = new AuthService();
            User emp = auth.login("david@company.com", "david123");
            AttendanceService att = new AttendanceService();

            // Clear today's test record if any to ensure clean test run
            Connection conn = DBConnection.getInstance().getConnection();
            try (Statement s = conn.createStatement()) {
                s.executeUpdate("DELETE FROM attendance WHERE user_id = " + emp.getUserId() + " AND record_date = CURDATE()");
            }

            // 1. Clock In
            AttendanceRecord record = att.clockIn(emp, "Testing live check-in");
            assertTrue(record != null && record.getClockIn() != null, "Clock in record must exist");

            // 2. Prevent duplicate clock-in
            boolean duplicateBlocked = false;
            try {
                att.clockIn(emp, "Duplicate check-in attempt");
            } catch (AttendanceException ex) {
                duplicateBlocked = true;
            }
            assertTrue(duplicateBlocked, "Duplicate check-in must be blocked with AttendanceException");

            // 3. Clock Out
            boolean clockedOut = att.clockOut(emp);
            assertTrue(clockedOut, "Clock out must return true");

            // 4. Verify in DB
            Optional<AttendanceRecord> todayOpt = att.getTodayRecord(emp.getUserId());
            assertTrue(todayOpt.isPresent(), "Today's record must be present");
            assertTrue(todayOpt.get().getClockOut() != null, "Clock out time must be stored");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testLeaveWorkflow() {
        try {
            AuthService auth = new AuthService();
            User emp = auth.login("emily@company.com", "emily123");
            User mgr = auth.login("manager@company.com", "manager123");
            LeaveService leaveService = new LeaveService();

            LocalDate start = LocalDate.now().plusDays(20);
            LocalDate end = LocalDate.now().plusDays(21); // 2 days

            // 1. Employee applies for leave
            LeaveRequest req = leaveService.applyLeave(emp, LeaveType.VACATION, start, end, "E2E Integration Test");
            assertTrue(req.getLeaveId() > 0, "Leave ID must be generated by DB");
            assertTrue(req.getStatus() == LeaveStatus.PENDING, "Initial status must be PENDING");

            // 2. Manager reviews and approves
            boolean approved = leaveService.reviewLeave(req.getLeaveId(), LeaveStatus.APPROVED, mgr);
            assertTrue(approved, "Leave review approval must succeed");

            // 3. Verify status in DB
            List<LeaveRequest> userLeaves = leaveService.getUserLeaves(emp.getUserId());
            boolean foundApproved = userLeaves.stream()
                    .anyMatch(l -> l.getLeaveId() == req.getLeaveId() && l.getStatus() == LeaveStatus.APPROVED);
            assertTrue(foundApproved, "Leave record must be marked APPROVED in DB");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testReportAndExport() {
        try {
            AuthService auth = new AuthService();
            User emp = auth.login("employee@company.com", "employee123");
            ReportService rpt = new ReportService();

            String reportText = rpt.generateUserMonthlyReport(emp.getUserId(), emp.getName());
            assertTrue(reportText != null && reportText.contains("EMPLOYEE TIME & ATTENDANCE SUMMARY REPORT"),
                    "Report text must contain header");

            // Test TXT file export
            File tempExport = new File("target/test_export.txt");
            tempExport.getParentFile().mkdirs();
            try (PrintWriter out = new PrintWriter(tempExport)) {
                out.print(reportText);
            }

            assertTrue(tempExport.exists() && tempExport.length() > 0, "Exported file must exist and not be empty");
            String readBack = Files.readString(tempExport.toPath());
            assertTrue(readBack.contains(emp.getName()), "Exported content must match generated report");
            tempExport.delete(); // Cleanup
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
