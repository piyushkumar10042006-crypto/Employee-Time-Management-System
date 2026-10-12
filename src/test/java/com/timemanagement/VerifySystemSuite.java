package com.timemanagement;

import com.timemanagement.exception.AttendanceException;
import com.timemanagement.exception.AuthenticationException;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.exception.InsufficientLeaveException;
import com.timemanagement.exception.TimeManagementException;
import com.timemanagement.model.Admin;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.Employee;
import com.timemanagement.model.LeaveRequest;
import com.timemanagement.model.Manager;
import com.timemanagement.model.User;
import com.timemanagement.model.enums.AttendanceStatus;
import com.timemanagement.model.enums.LeaveStatus;
import com.timemanagement.model.enums.LeaveType;
import com.timemanagement.model.enums.Role;
import com.timemanagement.service.SessionManager;
import com.timemanagement.thread.LiveClockThread;

import javax.swing.JLabel;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

/**
 * Automated Verification Test Suite.
 * Audits models, inheritance, polymorphism, calculations, multithreading,
 * session synchronization, and custom exception handling.
 */
public class VerifySystemSuite {

    private static int passedTests = 0;
    private static int totalTests = 0;

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("    Running Complete System Functionality Audit Suite        ");
        System.out.println("=============================================================");

        runTest("OOP Inheritance & Role Polymorphism", VerifySystemSuite::testUserPolymorphism);
        runTest("Encapsulation & Property Invariants", VerifySystemSuite::testEncapsulation);
        runTest("Attendance Hours & Overtime Calculations", VerifySystemSuite::testAttendanceCalculations);
        runTest("Leave Request Days & Status Workflow", VerifySystemSuite::testLeaveCalculations);
        runTest("Session Manager Synchronization", VerifySystemSuite::testSessionSynchronization);
        runTest("Multithreading Live Clock Lifecycle", VerifySystemSuite::testLiveClockThread);
        runTest("Custom Exception Hierarchy Integrity", VerifySystemSuite::testExceptionHierarchy);

        System.out.println("=============================================================");
        System.out.printf("Audit Results: %d / %d Tests Passed.\n", passedTests, totalTests);
        System.out.println("=============================================================");

        if (passedTests != totalTests) {
            System.exit(1);
        }
    }

    private static void runTest(String testName, Runnable testLogic) {
        totalTests++;
        try {
            testLogic.run();
            System.out.printf("  ✓ [PASS] %s\n", testName);
            passedTests++;
        } catch (Throwable t) {
            System.out.printf("  ❌ [FAIL] %s: %s\n", testName, t.getMessage());
            t.printStackTrace(System.out);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion Failed: " + message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError(String.format("%s: Expected [%s] but got [%s]", message, expected, actual));
        }
    }

    // 1. Test User Polymorphism and Inheritance
    private static void testUserPolymorphism() {
        User emp = new Employee(1, "Alice", "alice@company.com", "pass", "Engineering", 20, 30.0);
        User mgr = new Manager(2, "Bob", "bob@company.com", "pass", "Engineering", 22);
        User adm = new Admin(3, "Carol", "carol@company.com", "pass", "IT", 25);

        // Inheritance
        assertTrue(emp instanceof User, "Employee must extend User");
        assertTrue(mgr instanceof User, "Manager must extend User");
        assertTrue(adm instanceof User, "Admin must extend User");

        // Polymorphism
        assertEquals("Standard Employee", emp.getRoleBadge(), "Employee role badge");
        assertEquals("Department Manager", mgr.getRoleBadge(), "Manager role badge");
        assertEquals("System Administrator", adm.getRoleBadge(), "Admin role badge");

        assertTrue(!emp.canApproveLeaves(), "Employee cannot approve leaves");
        assertTrue(mgr.canApproveLeaves(), "Manager can approve leaves");
        assertTrue(adm.canApproveLeaves(), "Admin can approve leaves");

        assertTrue(!emp.canManageUsers(), "Employee cannot manage users");
        assertTrue(!mgr.canManageUsers(), "Manager cannot manage users");
        assertTrue(adm.canManageUsers(), "Admin can manage users");
    }

    // 2. Test Encapsulation & Validation Invariants
    private static void testEncapsulation() {
        Employee emp = new Employee();
        emp.setName("David");
        assertEquals("David", emp.getName(), "Name getter/setter");

        emp.setEmail("david@domain.com");
        assertEquals("david@domain.com", emp.getEmail(), "Email getter/setter");

        emp.setLeaveBalance(-5); // Must clamp to 0
        assertEquals(0, emp.getLeaveBalance(), "Leave balance must not be negative");

        boolean caughtInvalidEmail = false;
        try {
            emp.setEmail("invalid-email");
        } catch (IllegalArgumentException e) {
            caughtInvalidEmail = true;
        }
        assertTrue(caughtInvalidEmail, "Invalid email must throw IllegalArgumentException");
    }

    // 3. Test Attendance Hours and Overtime Calculations
    private static void testAttendanceCalculations() {
        AttendanceRecord record = new AttendanceRecord();
        record.setClockIn(LocalTime.of(9, 0));
        record.setClockOut(LocalTime.of(18, 30)); // 9.5 hours

        assertEquals(9.5, record.getHoursWorked(), "Total hours worked calculation");
        assertEquals(1.5, record.getOvertimeHours(), "Overtime hours calculation (> 8.0 hrs)");

        // Half-day calculation
        AttendanceRecord halfDayRecord = new AttendanceRecord();
        halfDayRecord.setClockIn(LocalTime.of(9, 0));
        halfDayRecord.setClockOut(LocalTime.of(13, 0)); // 4.0 hours

        assertEquals(4.0, halfDayRecord.getHoursWorked(), "Half day hours calculation");
        assertEquals(AttendanceStatus.HALF_DAY, halfDayRecord.getStatus(), "Status must be HALF_DAY when < 4.5 hrs");
    }

    // 4. Test Leave Request Calculations
    private static void testLeaveCalculations() {
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = LocalDate.now().plusDays(5);
        int days = (int) ChronoUnit.DAYS.between(start, end) + 1;
        assertEquals(4, days, "Day difference calculation inclusive");

        LeaveRequest req = new LeaveRequest();
        req.setLeaveType(LeaveType.VACATION);
        req.setStartDate(start);
        req.setEndDate(end);
        req.setDaysCount(days);

        assertEquals(LeaveStatus.PENDING, req.getStatus(), "Initial leave status must be PENDING");
        req.setStatus(LeaveStatus.APPROVED);
        assertEquals(LeaveStatus.APPROVED, req.getStatus(), "Updated status must be APPROVED");
    }

    // 5. Test Session Synchronization
    private static void testSessionSynchronization() {
        SessionManager session = SessionManager.getInstance();
        User testUser = new Employee(10, "Tester", "tester@test.com", "pass", "QA", 15, 20.0);

        session.login(testUser);
        assertTrue(session.isLoggedIn(), "Session must report logged in");
        assertEquals(testUser, session.getCurrentUser(), "Current user match");

        session.logout();
        assertTrue(!session.isLoggedIn(), "Session must report logged out after logout");
    }

    // 6. Test LiveClockThread Lifecycle
    private static void testLiveClockThread() {
        JLabel clockLbl = new JLabel();
        JLabel shiftLbl = new JLabel();
        LiveClockThread clock = new LiveClockThread(clockLbl, shiftLbl);

        clock.setPunchInTime(LocalTime.now().minusHours(1));
        clock.start();
        assertTrue(clock.isAlive(), "Live clock thread must be alive after start()");

        clock.clearPunchInTime();
        clock.stopClock();
        try {
            clock.join(1500); // Wait up to 1.5s for clean shutdown
        } catch (InterruptedException ignored) {}

        assertTrue(!clock.isAlive(), "Live clock thread must terminate cleanly when stopped");
    }

    // 7. Test Exception Hierarchy
    private static void testExceptionHierarchy() {
        TimeManagementException authEx = new AuthenticationException("Bad login");
        TimeManagementException attEx = new AttendanceException("Already clocked in");
        TimeManagementException leaveEx = new InsufficientLeaveException("Balance low");
        TimeManagementException dbEx = new DatabaseOperationException("SQL failed");

        assertTrue(authEx instanceof TimeManagementException, "AuthenticationException inherits base");
        assertTrue(attEx instanceof TimeManagementException, "AttendanceException inherits base");
        assertTrue(leaveEx instanceof TimeManagementException, "InsufficientLeaveException inherits base");
        assertTrue(dbEx instanceof TimeManagementException, "DatabaseOperationException inherits base");
    }
}
