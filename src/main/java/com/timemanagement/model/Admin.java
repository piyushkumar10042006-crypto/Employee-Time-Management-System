package com.timemanagement.model;

import com.timemanagement.model.enums.Role;

/**
 * Concrete Admin class inheriting from User.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public class Admin extends User {

    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    public Admin(int userId, String name, String email, String password, String department, int leaveBalance) {
        super(userId, name, email, password, Role.ADMIN, department, leaveBalance);
    }

    // Overridden Polymorphic Methods
    @Override
    public String getRoleBadge() {
        return "System Administrator";
    }

    @Override
    public boolean canApproveLeaves() {
        return true;
    }

    @Override
    public boolean canManageUsers() {
        return true; // Admins can create and delete user accounts
    }
}
