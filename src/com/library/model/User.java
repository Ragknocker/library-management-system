package com.library.model;

import java.time.LocalDateTime;

/**
 * Abstract Base Entity representing a person registered in the Library System.
 * Demonstrates OOP Inheritance and Polymorphism.
 */
public abstract class User {
    private int userId;
    private String name;
    private String email;
    private String phone;
    private LocalDateTime createdAt;

    public User() {
        this.createdAt = LocalDateTime.now();
    }

    public User(int userId, String name, String email, String phone) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = LocalDateTime.now();
    }

    // Abstract polymorphic methods implemented differently by Member & Librarian
    public abstract String getRoleDescription();
    public abstract int getMaxBorrowAllowance();
    public abstract int getDefaultLoanPeriodDays();

    // Encapsulation: getters and setters
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
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s)", getRoleDescription(), name, email);
    }
}
