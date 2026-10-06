-- ==============================================================================
-- Library Management System Database Schema
-- Target RDBMS: MySQL 8.0+
-- Storage Engine: InnoDB (Full ACID Support & Row-Level Locking)
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS library_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_db;

-- 1. Base Users Table (OOP Inheritance representation)
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(25) NOT NULL,
    user_role ENUM('STUDENT', 'FACULTY', 'COMMUNITY', 'LIBRARIAN') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (email),
    INDEX idx_user_role (user_role)
) ENGINE=InnoDB;

-- 2. Members Table (Polymorphic details for Library Card Holders)
CREATE TABLE IF NOT EXISTS members (
    member_id VARCHAR(50) PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    member_type ENUM('STUDENT', 'FACULTY', 'COMMUNITY') NOT NULL,
    max_borrow_limit INT NOT NULL DEFAULT 4,
    borrow_duration_days INT NOT NULL DEFAULT 14,
    active_loans_count INT NOT NULL DEFAULT 0,
    status ENUM('ACTIVE', 'SUSPENDED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_member_status (status)
) ENGINE=InnoDB;

-- 3. Books Catalog Table (Inventory Management for 1000+ Records)
CREATE TABLE IF NOT EXISTS books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    publisher VARCHAR(255) DEFAULT 'Unknown',
    publication_year INT NOT NULL,
    category VARCHAR(100) NOT NULL,
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    shelf_location VARCHAR(50) DEFAULT 'Main Stack',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_book_copies CHECK (available_copies >= 0 AND available_copies <= total_copies),
    INDEX idx_books_isbn (isbn),
    INDEX idx_books_title (title),
    INDEX idx_books_author (author),
    INDEX idx_books_category (category)
) ENGINE=InnoDB;

-- 4. Lending Loans Table (Issue & Return Transactions)
CREATE TABLE IF NOT EXISTS loans (
    loan_id INT AUTO_INCREMENT PRIMARY KEY,
    book_id INT NOT NULL,
    member_id VARCHAR(50) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE NULL,
    status ENUM('ACTIVE', 'RETURNED', 'OVERDUE') NOT NULL DEFAULT 'ACTIVE',
    renewal_count INT NOT NULL DEFAULT 0,
    issued_by VARCHAR(100) DEFAULT 'Head Librarian',
    notes VARCHAR(255) NULL,
    CONSTRAINT fk_loans_book FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE RESTRICT,
    CONSTRAINT fk_loans_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE RESTRICT,
    INDEX idx_loans_status (status),
    INDEX idx_loans_due_date (due_date),
    INDEX idx_loans_member (member_id),
    INDEX idx_loans_book (book_id)
) ENGINE=InnoDB;

-- 5. Fines Table (Overdue penalty and settlement tracking)
CREATE TABLE IF NOT EXISTS fines (
    fine_id INT AUTO_INCREMENT PRIMARY KEY,
    loan_id INT NOT NULL UNIQUE,
    member_id VARCHAR(50) NOT NULL,
    overdue_days INT NOT NULL,
    fine_rate DECIMAL(6,2) NOT NULL,
    total_fine DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('PENDING', 'PAID', 'WAIVED') NOT NULL DEFAULT 'PENDING',
    payment_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fines_loan FOREIGN KEY (loan_id) REFERENCES loans(loan_id) ON DELETE RESTRICT,
    CONSTRAINT fk_fines_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE RESTRICT,
    INDEX idx_fines_status (status),
    INDEX idx_fines_member (member_id)
) ENGINE=InnoDB;
