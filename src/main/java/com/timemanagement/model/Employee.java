package com.timemanagement.model;

import com.timemanagement.model.enums.Role;

/**
 * Concrete Employee class inheriting from User.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public class Employee extends User {

    private double hourlyRate;

    public Employee() {
        super();
        setRole(Role.EMPLOYEE);
        this.hourlyRate = 25.00;
    }

    public Employee(int userId, String name, String email, String password, String department, int leaveBalance, double hourlyRate) {
        super(userId, name, email, password, Role.EMPLOYEE, department, leaveBalance);
        this.hourlyRate = hourlyRate;
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = Math.max(0.0, hourlyRate);
    }

    // Overridden Polymorphic Methods
    @Override
    public String getRoleBadge() {
        return "Standard Employee";
    }

    @Override
    public boolean canApproveLeaves() {
        return false;
    }

    @Override
    public boolean canManageUsers() {
        return false;
    }
}
