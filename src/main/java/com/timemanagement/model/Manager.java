package com.timemanagement.model;

import com.timemanagement.model.enums.Role;

/**
 * Concrete Manager class inheriting from User.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public class Manager extends User {

    public Manager() {
        super();
        setRole(Role.MANAGER);
    }

    public Manager(int userId, String name, String email, String password, String department, int leaveBalance) {
        super(userId, name, email, password, Role.MANAGER, department, leaveBalance);
    }

    // Overridden Polymorphic Methods
    @Override
    public String getRoleBadge() {
        return "Department Manager";
    }

    @Override
    public boolean canApproveLeaves() {
        return true; // Managers can review and approve team leaves
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }
}
