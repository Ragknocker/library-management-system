# 📚 Athena Library Management System
> **Core Java Multi-Tier Enterprise Desktop Application**  
> Built with Core Java (Java 17/21/26), Java Swing GUI, JDBC, MySQL 8.0, and Object-Oriented Design (OOP).

---

## 🌟 Executive Overview & Resume Specifications

The **Athena Library Management System** is an enterprise-grade desktop application engineered to automate book cataloging, user accounts, and lending workflows for **1,000+ inventory records**. 

### Key Technical Achievements:
- **Multi-Tier Desktop Architecture**: Decouples presentation views (Java Swing) from domain business logic and persistence data layers.
- **Core OOP Principles**: Implements class inheritance, runtime polymorphism, encapsulation invariants, and the **DAO (Data Access Object) Pattern**.
- **ACID-Compliant Concurrency Control**: Eliminates race conditions during simultaneous book checkouts using atomic SQL updates and strict transactional boundaries (`setAutoCommit(false)`, `commit()`, `rollback()`).
- **Automated Input Validation & Custom Exception Hierarchy**: Comprehensive field regex, checksum checks, and domain exception handling preventing runtime crashes and guaranteeing 100% transactional consistency.
- **1,000+ Record Inventory Seeder**: High-performance JDBC batch insertion engine generating 1,000+ realistic book records, multi-tiered patrons, and lending transactions.

---

## 🏛️ System Architecture & Tiers

```
+-----------------------------------------------------------------------+
|                       PRESENTATION TIER (Swing GUI)                    |
|  - MainFrame (Navigation, Header, Live DB Status Indicator)            |
|  - DashboardPanel (Live Metric Cards, Quick Actions, Recent Activity)  |
|  - BookCatalogPanel (1,000+ Records Table, Pagination, Search, CRUD)   |
|  - LendingDeskPanel (ACID Issue/Checkout Desk, Return/Renew Desk)      |
|  - MemberManagementPanel (Patron Profiles, Tier Rules, Status)        |
|  - OverdueFinesPanel (Overdue Loan Tracker, Fine Settlements)         |
|  - ReportsPanel (Category Analytics, CSV Audit Log Exporter)          |
|  - SettingsDialog (MySQL Host/User/Pass Config & Auto Seeder)          |
+-----------------------------------------------------------------------+
                                    │
                                    ▼
+-----------------------------------------------------------------------+
|                        BUSINESS & SERVICE TIER                         |
|  - BookService: Validates & orchestrates catalog inventory            |
|  - MemberService: Manages card holders, borrow quotas, and accounts   |
|  - LendingService: Enforces ACID transactions & concurrency locking   |
|  - ReportService: Aggregates system metrics and financial audits      |
|  - InputValidator: ISBN-10/13 checksums, email RFC, phone, quantities  |
+-----------------------------------------------------------------------+
                                    │
                                    ▼
+-----------------------------------------------------------------------+
|                    DATA ACCESS LAYER (DAO PATTERN)                    |
|  - IBookDAO       <─── BookDAOImpl       (Parameterized Queries)      |
|  - IMemberDAO     <─── MemberDAOImpl     (Inheritance Join Queries)   |
|  - ILoanDAO       <─── LoanDAOImpl       (Transactional Issue/Return) |
|  - IFineDAO       <─── FineDAOImpl       (Penalty Audits)             |
+-----------------------------------------------------------------------+
                                    │
                                    ▼
+-----------------------------------------------------------------------+
|                   DATABASE TIER (MySQL 8.0 / InnoDB)                   |
|  - books (1,000+ records, stock counters, check constraints)          |
|  - users & members (Inheritance mapping, 1-to-1 normalized tables)     |
|  - loans (Foreign keys, indexed dates, status flags)                  |
|  - fines (Penalty calculations, settlement timestamps)                 |
+-----------------------------------------------------------------------+
```

---

## 🧬 Object-Oriented Design (OOP) Principles Applied

### 1. Inheritance
- `abstract class User`: Defines universal identity attributes (`userId`, `name`, `email`, `phone`, `createdAt`).
- `class Member extends User`: Represents student/faculty card holders with specific lending limits and borrowing periods.
- `class Librarian extends User`: Represents library personnel and system administrators.

### 2. Polymorphism
Dynamic method dispatch is implemented across the user hierarchy:
```java
// Abstract methods in User dynamically evaluated at runtime
public abstract String getRoleDescription();
public abstract int getMaxBorrowAllowance();
public abstract int getDefaultLoanPeriodDays();
```
- A **Student Member** gets 4 books maximum and 14 days per loan.
- A **Faculty Member** gets 10 books maximum and 30 days per loan.
- A **Librarian** gets 20 books maximum and 60 days per loan.

### 3. DAO Pattern (Data Access Object)
All database interactions are defined through abstract interfaces (`IBookDAO`, `IMemberDAO`, `ILoanDAO`, `IFineDAO`). The UI panels and business services interact exclusively with the interfaces, allowing painless swapping or testing of database implementations without affecting presentation logic.

### 4. Custom Exception Hierarchy
```
Exception
 └── LibraryException (Checked Base Exception)
      ├── ValidationException (Input field errors)
      ├── BookNotFoundException
      ├── BookUnavailableException (Out-of-stock / concurrency conflict)
      ├── MemberNotFoundException
      ├── MemberLimitExceededException (Borrowing quota exceeded)
      ├── ActiveLoanExistsException (Duplicate active checkout)
      └── DatabaseException (SQL & Transaction failures)
```

---

## 🔒 ACID Compliance & Concurrency Control

In high-concurrency library systems, multiple patrons or librarians may attempt to check out the last remaining copy of a book at the same time. Athena eliminates race conditions through **Atomic Conditional Database Queries** combined with **JDBC Transaction Isolation**:

```java
// 1. Begin ACID Transaction Scope
Connection conn = DBConnection.getInstance().getConnection();
conn.setAutoCommit(false);
conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

try {
    // 2. Concurrency-Safe Atomic Decrement
    // If available_copies == 0, query updates 0 rows and returns false
    boolean copyDeducted = bookDAO.decrementAvailableCopies(conn, bookId);
    if (!copyDeducted) {
        conn.rollback();
        throw new BookUnavailableException("Book is out of stock or reserved by another transaction.");
    }

    // 3. Update Member Active Loan Count
    memberDAO.updateActiveLoansCount(conn, member.getMemberId(), 1);

    // 4. Record Lending Transaction
    loanDAO.issueBook(conn, loan);

    // 5. Commit Transaction
    conn.commit();
} catch (SQLException e) {
    conn.rollback(); // Rollback on any failure
    throw new DatabaseException("Transaction Failed", e);
} finally {
    conn.setAutoCommit(true);
    conn.close();
}
```

---

## 📊 Database Schema (`library_db`)

The schema is configured with the MySQL **InnoDB Engine** for foreign key enforcement, row-level locking, and transactional rollback:

1. **`books`**: `book_id`, `isbn` (UNIQUE), `title`, `author`, `publisher`, `publication_year`, `category`, `total_copies`, `available_copies`, `shelf_location`, `CONSTRAINT chk_book_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)`.
2. **`users`**: Base user entity storing `user_id`, `name`, `email` (UNIQUE), `phone`, `user_role`.
3. **`members`**: Relational extension linked via `user_id` foreign key: `member_id` (PK), `member_type`, `max_borrow_limit`, `borrow_duration_days`, `active_loans_count`, `status`.
4. **`loans`**: Checkout transactions: `loan_id`, `book_id` (FK), `member_id` (FK), `issue_date`, `due_date`, `return_date`, `status` (`ACTIVE`, `RETURNED`, `OVERDUE`), `renewal_count`.
5. **`fines`**: Overdue fines: `fine_id`, `loan_id` (FK), `member_id` (FK), `overdue_days`, `fine_rate`, `total_fine`, `paid_amount`, `status` (`PENDING`, `PAID`).

---

## 🚀 Quick Start & Execution Guide

### Prerequisites
- **Java SE Development Kit**: JDK 17, 21, or 26 installed (detected on this system).
- **MySQL Server 8.0**: Service running on `localhost:3306`.

### Project Structure
```
LibraryManagementSystem/
├── bin/                          # Compiled bytecode classes
├── lib/
│   ├── mysql-connector-j-8.4.0.jar  # Official MySQL JDBC Driver
│   └── flatlaf-3.5.4.jar         # Modern Look and Feel UI theme
├── resources/
│   ├── db.properties             # Database credentials & policy configuration
│   └── schema.sql                # Complete relational schema DDL
├── src/
│   └── com/library/
│       ├── Main.java             # Entry point
│       ├── config/               # AppConfig, DBConnection
│       ├── dao/                  # Interfaces and JDBC Implementations
│       ├── exception/            # Custom Exception Hierarchy
│       ├── model/                # User, Member, Book, Loan, Fine
│       ├── seeder/               # DataSeeder (1,000+ records)
│       ├── service/              # Business Logic & Transactions
│       ├── test/                 # SystemValidationTest suite
│       ├── ui/                   # MainFrame and Swing Panels
│       └── util/                 # InputValidator, DateUtil, UITheme
├── build.bat                     # Compiles with javac
├── run.bat                       # Runs the desktop GUI
├── test.bat                      # Runs the automated test suite
├── pom.xml                       # Maven configuration
└── README.md
```

### Running the Application

1. **Compile the Project**:
   ```cmd
   build.bat
   ```

2. **Run Automated Tests**:
   ```cmd
   test.bat
   ```
   *Executes 21 unit & invariant tests verifying OOP polymorphism, ISBN checksums, regex validators, and exception hierarchies.*

3. **Launch the Desktop Application**:
   ```cmd
   run.bat
   ```

4. **Connecting to MySQL**:
   - On first launch, if MySQL credentials differ from `root / root`, click the **`⚙ DB Settings`** button on the top right.
   - Enter your MySQL password.
   - Click **`Test Connection`**.
   - Click **`⚡ Initialize & Seed 1,000 Records`** to automatically create tables and populate 1,000+ realistic book inventory records and members!

---

## 🧪 Automated Test Suite Verification

Run `test.bat` to see instant validation of:
- **OOP Polymorphic Allowance**: Student (4 books / 14 days), Faculty (10 books / 30 days), Librarian (20 books / 60 days).
- **Strict Input Validation**: Negative inventory rejected, invalid stock limits prevented.
- **ISBN Checksum Logic**: Validates mathematical checksums for both ISBN-10 and ISBN-13; rejects invalid check digits.
- **Exception Class Polymorphism**: Verifies hierarchy of `BookNotFoundException`, `BookUnavailableException`, `MemberLimitExceededException`.
- **Date Invariants**: Computes overdue day offsets and fine calculations.
