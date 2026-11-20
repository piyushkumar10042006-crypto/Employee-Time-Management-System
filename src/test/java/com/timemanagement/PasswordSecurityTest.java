package com.timemanagement;

import com.timemanagement.util.PasswordUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying BCrypt password hashing, salt variation,
 * verification, and double-hashing prevention.
 */
class PasswordSecurityTest {

    @Test
    @DisplayName("Hash plain text produces valid 60-character BCrypt hash")
    void testBCryptFormat() {
        String plain = "SecretPass123!";
        String hash = PasswordUtils.hashPassword(plain);

        assertNotNull(hash);
        assertEquals(60, hash.length(), "BCrypt hash length must be 60 characters");
        assertTrue(PasswordUtils.isBCryptHash(hash), "Must be recognized as BCrypt hash");
    }

    @Test
    @DisplayName("Salt generation ensures identical passwords produce unique hashes")
    void testSaltUniqueness() {
        String plain = "employee123";
        String hash1 = PasswordUtils.hashPassword(plain);
        String hash2 = PasswordUtils.hashPassword(plain);

        assertNotEquals(hash1, hash2, "BCrypt salts must differ on every hash invocation");
        assertTrue(PasswordUtils.verifyPassword(plain, hash1), "Hash 1 must verify plain password");
        assertTrue(PasswordUtils.verifyPassword(plain, hash2), "Hash 2 must verify plain password");
    }

    @Test
    @DisplayName("Verification succeeds with correct password and fails with wrong password")
    void testPasswordVerification() {
        String plain = "manager123";
        String hash = PasswordUtils.hashPassword(plain);

        assertTrue(PasswordUtils.verifyPassword(plain, hash), "Correct password must verify");
        assertFalse(PasswordUtils.verifyPassword("wrongpassword", hash), "Wrong password must fail");
        assertFalse(PasswordUtils.verifyPassword(null, hash), "Null password must fail");
    }

    @Test
    @DisplayName("Already hashed password is never re-hashed (idempotence)")
    void testAvoidDoubleHashing() {
        String plain = "admin123";
        String hash = PasswordUtils.hashPassword(plain);

        // Hashing an already hashed string should return the exact same hash
        String rehashed = PasswordUtils.hashPassword(hash);
        assertEquals(hash, rehashed, "Calling hashPassword on existing BCrypt hash must return it unchanged");
    }

    @Test
    @DisplayName("Legacy plain text password successfully verifies for migration")
    void testLegacyPlainTextFallback() {
        String legacyPlain = "oldPlainPassword";

        // When stored password is plain text, verifyPassword checks equality
        assertTrue(PasswordUtils.verifyPassword(legacyPlain, legacyPlain), "Legacy plain text should verify");
        assertFalse(PasswordUtils.verifyPassword("wrong", legacyPlain), "Wrong password should fail");
    }

    @Test
    @DisplayName("On-login migration upgrades plain-text password to BCrypt in database conditionally")
    void testOnLoginMigration() throws Exception {
        String testUrl = "jdbc:mysql://localhost:3306/employee_time_test_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
        com.timemanagement.db.DBConnection.reinitializeForDatabase(testUrl);
        com.timemanagement.dao.UserDAO userDAO = new com.timemanagement.dao.UserDAO();

        String email = "plain_user_" + System.currentTimeMillis() + "@test.com";

        // Insert user directly with plain text password
        try (java.sql.Connection conn = com.timemanagement.db.DBConnection.getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO users (name, email, password, role, department, hourly_rate, leave_balance) VALUES (?, ?, ?, 'EMPLOYEE', 'Test', 25.0, 20)")) {
            stmt.setString(1, "Plain User");
            stmt.setString(2, email);
            stmt.setString(3, "mypassword123");
            stmt.executeUpdate();
        }

        // Authenticate with plain password
        java.util.Optional<com.timemanagement.model.User> userOpt = userDAO.authenticate(email, "mypassword123");
        assertTrue(userOpt.isPresent(), "User should authenticate with plain password");

        // Verify that database password was upgraded to BCrypt
        try (java.sql.Connection conn = com.timemanagement.db.DBConnection.getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement("SELECT password FROM users WHERE email = ?")) {
            stmt.setString(1, email);
            try (java.sql.ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next());
                String stored = rs.getString("password");
                assertTrue(PasswordUtils.isBCryptHash(stored), "Password in DB must now be a BCrypt hash");
                assertTrue(PasswordUtils.verifyPassword("mypassword123", stored), "BCrypt hash must verify mypassword123");
            }
        }

        // Subsequent authentication against new BCrypt hash
        java.util.Optional<com.timemanagement.model.User> secondLogin = userDAO.authenticate(email, "mypassword123");
        assertTrue(secondLogin.isPresent(), "Subsequent login against BCrypt hash must succeed");

        // Cleanup
        try (java.sql.Connection conn = com.timemanagement.db.DBConnection.getInstance().getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE email = ?")) {
            stmt.setString(1, email);
            stmt.executeUpdate();
        }
    }
}
