package com.library.model;

/**
 * Staff entity representing a Librarian or Administrator.
 * Inherits from User and overrides polymorphic methods.
 */
public class Librarian extends User {
    private String employeeId;
    private String department;
    private boolean adminAccess;

    public Librarian() {
        super();
        this.adminAccess = false;
    }

    public Librarian(int userId, String name, String email, String phone,
                     String employeeId, String department, boolean adminAccess) {
        super(userId, name, email, phone);
        this.employeeId = employeeId;
        this.department = department;
        this.adminAccess = adminAccess;
    }

    @Override
    public String getRoleDescription() {
        return adminAccess ? "Head Librarian (Admin)" : "Assistant Librarian";
    }

    @Override
    public int getMaxBorrowAllowance() {
        return 20; // Librarians have special high allowance
    }

    @Override
    public int getDefaultLoanPeriodDays() {
        return 60;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean hasAdminAccess() {
        return adminAccess;
    }

    public void setAdminAccess(boolean adminAccess) {
        this.adminAccess = adminAccess;
    }
}
