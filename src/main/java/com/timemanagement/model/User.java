package com.timemanagement.model;

import com.timemanagement.model.enums.Role;

/**
 * Abstract base class representing a User in the system.
 * Demonstrates OOP Inheritance, Encapsulation, and Polymorphism.
 */
public abstract class User {

    private int userId;
    private String name;
    private String email;
    private String password;
    private Role role;
    private String department;
    private int leaveBalance;

    // Default Constructor
    public User() {
        this.leaveBalance = 20;
    }

    // Parameterized Constructor
    public User(int userId, String name, String email, String password, Role role, String department, int leaveBalance) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.department = department;
        this.leaveBalance = leaveBalance;
    }

    // Polymorphic Abstract Methods
    public abstract String getRoleBadge();
    public abstract boolean canApproveLeaves();
    public abstract boolean canManageUsers();

    // Encapsulation: Getters and Setters with basic validation
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = email.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getLeaveBalance() {
        return leaveBalance;
    }

    public void setLeaveBalance(int leaveBalance) {
        this.leaveBalance = Math.max(0, leaveBalance);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s - %s)", role, name, email, department);
    }
}
