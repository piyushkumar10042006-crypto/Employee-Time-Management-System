package com.timemanagement.dao;

import com.timemanagement.db.DBConnection;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.model.Admin;
import com.timemanagement.model.Employee;
import com.timemanagement.model.Manager;
import com.timemanagement.model.User;
import com.timemanagement.model.enums.Role;
import com.timemanagement.util.PasswordUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for User operations using pooled JDBC connections.
 * Enforces on-login BCrypt migration, conditional updates, and try-with-resources.
 */
public class UserDAO implements GenericDAO<User, Integer> {

    @Override
    public User save(User user) throws DatabaseOperationException {
        String sql = "INSERT INTO users (name, email, password, role, department, hourly_rate, leave_balance) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            // Ensure passwords are always hashed with BCrypt upon creation
            stmt.setString(3, PasswordUtils.hashPassword(user.getPassword()));
            stmt.setString(4, user.getRole().name());
            stmt.setString(5, user.getDepartment());
            
            double hourlyRate = 25.0;
            if (user instanceof Employee) {
                hourlyRate = ((Employee) user).getHourlyRate();
            }
            stmt.setDouble(6, hourlyRate);
            stmt.setInt(7, user.getLeaveBalance());

            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
            }
            return user;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error saving user: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Optional<User> findById(Integer id) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error finding user by ID: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error finding user by email: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /**
     * Authenticates a user securely. Supports BCrypt password verification with
     * on-login conditional migration for legacy plain-text passwords.
     */
    public Optional<User> authenticate(String email, String password) throws DatabaseOperationException {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    int userId = rs.getInt("user_id");

                    // 1. Verify credentials using PasswordUtils
                    if (PasswordUtils.verifyPassword(password, storedPassword)) {
                        // 2. If password was plain text, perform conditional upgrade to BCrypt
                        if (!PasswordUtils.isBCryptHash(storedPassword)) {
                            migratePasswordOnLogin(userId, storedPassword, password);
                        }
                        return Optional.of(mapRowToUser(rs));
                    }
                }
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Authentication query error: " + ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    /**
     * Performs a conditional update to safely migrate plain text password to BCrypt
     * without concurrent conflict.
     */
    private void migratePasswordOnLogin(int userId, String oldPassword, String plainPassword) {
        String updateSql = "UPDATE users SET password = ? WHERE user_id = ? AND password = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setString(1, PasswordUtils.hashPassword(plainPassword));
            stmt.setInt(2, userId);
            stmt.setString(3, oldPassword); // Conditional match prevents race condition
            stmt.executeUpdate();
        } catch (Exception ex) {
            // Non-blocking: log notice and allow successful login
            System.err.println("Notice: On-login password migration notice: " + ex.getMessage());
        }
    }

    @Override
    public List<User> findAll() throws DatabaseOperationException {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY user_id ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                userList.add(mapRowToUser(rs));
            }
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error fetching all users: " + ex.getMessage(), ex);
        }
        return userList;
    }

    @Override
    public boolean update(User user) throws DatabaseOperationException {
        String sql = "UPDATE users SET name = ?, email = ?, department = ?, leave_balance = ? WHERE user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getDepartment());
            stmt.setInt(4, user.getLeaveBalance());
            stmt.setInt(5, user.getUserId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error updating user: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean delete(Integer id) throws DatabaseOperationException {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new DatabaseOperationException("Error deleting user: " + ex.getMessage(), ex);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        Role role = Role.valueOf(rs.getString("role"));
        String department = rs.getString("department");
        double hourlyRate = rs.getDouble("hourly_rate");
        int leaveBalance = rs.getInt("leave_balance");

        switch (role) {
            case ADMIN:
                return new Admin(id, name, email, password, department, leaveBalance);
            case MANAGER:
                return new Manager(id, name, email, password, department, leaveBalance);
            case EMPLOYEE:
            default:
                return new Employee(id, name, email, password, department, leaveBalance, hourlyRate);
        }
    }
}
