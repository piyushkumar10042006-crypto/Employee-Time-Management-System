package com.timemanagement;

import com.timemanagement.db.DBConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests verifying HikariCP connection leasing,
 * multi-threaded borrowing, and clean return without pool exhaustion.
 */
class ConnectionPoolConcurrencyTest {

    private static final String TEST_DB_URL = "jdbc:mysql://localhost:3306/employee_time_test_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";

    @BeforeAll
    static void setup() throws Exception {
        DBConnection.reinitializeForDatabase(TEST_DB_URL);
    }

    @Test
    @DisplayName("20 concurrent threads borrowing and returning connections simultaneously")
    void testConcurrentConnectionBorrowing() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to execute simultaneously

                    // Borrow connection, perform query, return connection
                    try (Connection conn = DBConnection.getInstance().getConnection();
                         PreparedStatement stmt = conn.prepareStatement("SELECT 1");
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 1) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All 20 threads should finish within 15 seconds without deadlock");
        assertEquals(threadCount, successCount.get(), "All 20 threads should execute successfully");
    }
}
