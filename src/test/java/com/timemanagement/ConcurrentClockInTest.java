package com.timemanagement;

import com.timemanagement.dao.AttendanceDAO;
import com.timemanagement.dao.UserDAO;
import com.timemanagement.db.DBConnection;
import com.timemanagement.db.DBInitializer;
import com.timemanagement.exception.AttendanceException;
import com.timemanagement.model.AttendanceRecord;
import com.timemanagement.model.Employee;
import com.timemanagement.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests verifying that concurrent duplicate clock-ins are blocked
 * at the database level using transactions and unique constraints.
 * Runs exclusively against employee_time_test_db.
 */
class ConcurrentClockInTest {

    private static final String TEST_DB_URL = "jdbc:mysql://localhost:3306/employee_time_test_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    private static User testUser;
    private static AttendanceDAO attendanceDAO;
    private static UserDAO userDAO;

    @BeforeAll
    static void setup() throws Exception {
        DBConnection.reinitializeForDatabase(TEST_DB_URL);
        DBInitializer.initializeDatabase();

        userDAO = new UserDAO();
        attendanceDAO = new AttendanceDAO();

        // Create isolated test user with unique email
        String email = "concurrent_test_" + System.currentTimeMillis() + "@test.com";
        User emp = new Employee(0, "Concurrent Tester", email, "testpass123", "Engineering", 20, 30.0);
        testUser = userDAO.save(emp);
    }

    @AfterAll
    static void cleanup() {
        if (testUser != null && testUser.getUserId() > 0) {
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM attendance WHERE user_id = " + testUser.getUserId());
                stmt.executeUpdate("DELETE FROM users WHERE user_id = " + testUser.getUserId());
            } catch (Exception ignored) {}
        }
    }

    @Test
    @DisplayName("5 concurrent threads clocking in simultaneously: exactly 1 succeeds, 4 are blocked")
    void testConcurrentClockInPrevention() throws Exception {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);

        LocalDateTime punchTime = LocalDateTime.now().withNano(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Synchronize all threads to hit DB at the exact same millisecond
                    attendanceDAO.clockIn(testUser.getUserId(), punchTime, "Concurrent test punch");
                    successCount.incrementAndGet();
                } catch (AttendanceException ex) {
                    blockedCount.incrementAndGet();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire all 5 threads simultaneously
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete");
        assertEquals(1, successCount.get(), "Exactly 1 clock-in must succeed");
        assertEquals(4, blockedCount.get(), "Exactly 4 concurrent clock-ins must be blocked");

        // Verify active shift exists in database
        Optional<AttendanceRecord> active = attendanceDAO.findActiveShift(testUser.getUserId());
        assertTrue(active.isPresent(), "Active shift record must exist");

        // Clock out and verify shift is closed cleanly
        boolean clockedOut = attendanceDAO.clockOut(active.get().getRecordId(), punchTime.plusHours(8));
        assertTrue(clockedOut, "Clock out must succeed");

        Optional<AttendanceRecord> postClockOut = attendanceDAO.findActiveShift(testUser.getUserId());
        assertFalse(postClockOut.isPresent(), "No active shift should remain after clock out");
    }
}
