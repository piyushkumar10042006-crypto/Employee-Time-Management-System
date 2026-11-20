package com.timemanagement;

import com.timemanagement.dao.LeaveDAO;
import com.timemanagement.dao.UserDAO;
import com.timemanagement.db.DBConnection;
import com.timemanagement.db.DBInitializer;
import com.timemanagement.model.Employee;
import com.timemanagement.model.LeaveRequest;
import com.timemanagement.model.User;
import com.timemanagement.model.enums.LeaveStatus;
import com.timemanagement.model.enums.LeaveType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying atomic transaction commit and rollback behavior.
 * Ensures leave deductions are rolled back on failure and never corrupt user balances.
 * Runs exclusively against employee_time_test_db.
 */
class TransactionRollbackTest {

    private static final String TEST_DB_URL = "jdbc:mysql://localhost:3306/employee_time_test_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static User testUser;
    private static UserDAO userDAO;
    private static LeaveDAO leaveDAO;

    @BeforeAll
    static void setup() throws Exception {
        DBConnection.reinitializeForDatabase(TEST_DB_URL);
        DBInitializer.initializeDatabase();

        userDAO = new UserDAO();
        leaveDAO = new LeaveDAO();

        String email = "tx_test_" + System.currentTimeMillis() + "@test.com";
        User emp = new Employee(0, "Transaction Tester", email, "pass123", "IT", 20, 25.0);
        testUser = userDAO.save(emp);
    }

    @AfterAll
    static void cleanup() {
        if (testUser != null && testUser.getUserId() > 0) {
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM leave_requests WHERE user_id = " + testUser.getUserId());
                stmt.executeUpdate("DELETE FROM users WHERE user_id = " + testUser.getUserId());
            } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("Transaction Commit: Leave approval deducts balance atomically")
    void testTransactionCommitDeductsBalance() throws Exception {
        // Initial balance = 20
        assertEquals(20, testUser.getLeaveBalance());

        LeaveRequest req = new LeaveRequest();
        req.setUserId(testUser.getUserId());
        req.setLeaveType(LeaveType.CASUAL);
        req.setStartDate(LocalDate.now().plusDays(10));
        req.setEndDate(LocalDate.now().plusDays(12));
        req.setDaysCount(3);
        req.setReason("Transaction Test");
        req.setStatus(LeaveStatus.PENDING);

        LeaveRequest created = leaveDAO.applyLeave(req);
        assertTrue(created.getLeaveId() > 0);

        // Approve leave
        boolean approved = leaveDAO.updateStatus(created.getLeaveId(), LeaveStatus.APPROVED, 1);
        assertTrue(approved);

        // Verify balance in database is now 17 (20 - 3)
        Optional<User> updatedUser = userDAO.findById(testUser.getUserId());
        assertTrue(updatedUser.isPresent());
        assertEquals(17, updatedUser.get().getLeaveBalance(), "Leave balance must be deducted to 17");
    }

    @Test
    @DisplayName("Transaction Rollback: Invalid leave ID triggers rollback without deducting balance")
    void testTransactionRollbackOnInvalidLeave() throws Exception {
        int balanceBefore = userDAO.findById(testUser.getUserId()).get().getLeaveBalance();

        // Updating non-existent leave ID must fail safely and rollback
        int nonExistentLeaveId = 9999999;
        boolean result = leaveDAO.updateStatus(nonExistentLeaveId, LeaveStatus.APPROVED, 1);
        assertFalse(result, "Updating non-existent leave ID must return false");

        // Verify balance remains completely unchanged
        int balanceAfter = userDAO.findById(testUser.getUserId()).get().getLeaveBalance();
        assertEquals(balanceBefore, balanceAfter, "User leave balance must remain untouched after rollback");
    }
}
