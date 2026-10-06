package com.library.seeder;

import com.library.config.DBConnection;
import com.library.model.MemberType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Random;

/**
 * High-performance Automated Data Seeder.
 * Generates 1,000+ realistic inventory books, sample user accounts,
 * and lending transactions using JDBC batching.
 */
public class DataSeeder {

    private static final String[] CATEGORIES = {
            "Computer Science", "Artificial Intelligence", "Data Science",
            "Software Engineering", "Mathematics", "Physics",
            "Classic Literature", "World History", "Philosophy",
            "Economics", "Psychology", "Biotechnology"
    };

    private static final String[] TITLE_PREFIXES = {
            "Principles of", "Advanced", "Introduction to", "The Art of",
            "Mastering", "Modern", "Fundamentals of", "Practical Guide to",
            "Deep Dive into", "Theoretical", "Comprehensive Study on", "Handbook of",
            "Applied", "Explorations in", "Foundations of", "Essential"
    };

    private static final String[] TITLE_SUBJECTS = {
            "Algorithms and Data Structures", "Distributed Cloud Architecture", "Database Systems Design",
            "Deep Learning and Neural Nets", "Compiler Construction", "Operating Systems Internals",
            "Quantum Computation", "Cybersecurity and Cryptography", "Computer Vision and Perception",
            "Statistical Mechanics", "Linear Algebra and Vector Calculus", "Modern Monetary Economics",
            "Ancient Civilizations and Empires", "Moral Philosophy and Ethics", "Cognitive Behavioral Psychology",
            "Molecular Genetics and CRISPR", "Object-Oriented Software Design", "Microservices and DevOps"
    };

    private static final String[] AUTHORS = {
            "Donald Knuth", "Martin Fowler", "Robert C. Martin", "Andrew S. Tanenbaum",
            "Stuart Russell", "Peter Norvig", "Thomas H. Cormen", "Charles Leiserson",
            "Brian Kernighan", "Dennis Ritchie", "Bjarne Stroustrup", "Joshua Bloch",
            "Eric Evans", "Ian Goodfellow", "Yoshua Bengio", "Richard Feynman",
            "Stephen Hawking", "Yuval Noah Harari", "Daniel Kahneman", "Carl Sagan"
    };

    private static final String[] PUBLISHERS = {
            "Addison-Wesley", "O'Reilly Media", "MIT Press", "Prentice Hall",
            "Springer", "Cambridge University Press", "Oxford University Press",
            "McGraw-Hill Education", "Wiley", "HarperCollins", "Penguin Books"
    };

    private static final String[] SHELVES = {
            "Stack A-01", "Stack A-02", "Stack B-03", "Stack B-04",
            "Stack C-01", "Stack C-05", "Stack D-02", "Reference Room",
            "Rare Books Rm", "Main Reading Hall"
    };

    /**
     * Seeds the database with 1,000+ books and starter members/loans.
     */
    public static void seedAll(Connection conn, int targetBookCount) throws Exception {
        conn.setAutoCommit(false);

        // 1. Check if books already seeded
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books")) {
            if (rs.next() && rs.getInt(1) >= 1000) {
                System.out.println("Database already contains " + rs.getInt(1) + " books. Skipping book seed.");
                return;
            }
        }

        System.out.println("Seeding " + targetBookCount + " inventory books via JDBC batching...");

        // 2. Batch insert books
        String bookSql = "INSERT INTO books (isbn, title, author, publisher, publication_year, category, total_copies, available_copies, shelf_location) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Random rand = new Random(42); // deterministic seed for reproducibility

        try (PreparedStatement psBook = conn.prepareStatement(bookSql)) {
            for (int i = 1; i <= targetBookCount; i++) {
                String prefix = TITLE_PREFIXES[rand.nextInt(TITLE_PREFIXES.length)];
                String subject = TITLE_SUBJECTS[rand.nextInt(TITLE_SUBJECTS.length)];
                String title = prefix + " " + subject + " (Vol. " + (rand.nextInt(5) + 1) + ")";
                String author = AUTHORS[rand.nextInt(AUTHORS.length)];
                String publisher = PUBLISHERS[rand.nextInt(PUBLISHERS.length)];
                String category = CATEGORIES[rand.nextInt(CATEGORIES.length)];
                String shelf = SHELVES[rand.nextInt(SHELVES.length)];
                int year = 1980 + rand.nextInt(46); // 1980 to 2026

                // Valid synthetic 13-digit ISBN
                long isbnBody = 978000000000L + (long) i;
                // calculate ISBN-13 check digit
                String s12 = String.valueOf(isbnBody);
                int sum = 0;
                for (int j = 0; j < 12; j++) {
                    int d = s12.charAt(j) - '0';
                    sum += (j % 2 == 0) ? d : d * 3;
                }
                int check = (10 - (sum % 10)) % 10;
                String isbn = s12 + check;

                int totalCopies = rand.nextInt(5) + 2; // 2 to 6 copies
                int availableCopies = totalCopies;

                psBook.setString(1, isbn);
                psBook.setString(2, title);
                psBook.setString(3, author);
                psBook.setString(4, publisher);
                psBook.setInt(5, year);
                psBook.setString(6, category);
                psBook.setInt(7, totalCopies);
                psBook.setInt(8, availableCopies);
                psBook.setString(9, shelf);
                psBook.addBatch();

                if (i % 250 == 0) {
                    psBook.executeBatch();
                }
            }
            psBook.executeBatch();
        }

        // 3. Seed Sample Users & Members
        String userSql = "INSERT INTO users (name, email, phone, user_role) VALUES (?, ?, ?, ?)";
        String memberSql = "INSERT INTO members (member_id, user_id, member_type, max_borrow_limit, borrow_duration_days, active_loans_count, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

        String[][] sampleMembers = {
                {"Alice Johnson", "alice.johnson@university.edu", "+1-555-0101", "STUDENT", "LIB-STU-1001"},
                {"Prof. Robert Davis", "r.davis@faculty.edu", "+1-555-0102", "FACULTY", "LIB-FAC-2001"},
                {"Clara Oswald", "clara.o@university.edu", "+1-555-0103", "STUDENT", "LIB-STU-1002"},
                {"David Miller", "d.miller@community.org", "+1-555-0104", "COMMUNITY", "LIB-COM-3001"},
                {"Emma Watson", "e.watson@university.edu", "+1-555-0105", "STUDENT", "LIB-STU-1003"},
                {"Dr. James Wilson", "j.wilson@faculty.edu", "+1-555-0106", "FACULTY", "LIB-FAC-2002"},
                {"Grace Hopper", "g.hopper@polytech.edu", "+1-555-0107", "STUDENT", "LIB-STU-1004"},
                {"Henry Ford", "h.ford@university.edu", "+1-555-0108", "STUDENT", "LIB-STU-1005"}
        };

        try (PreparedStatement psUser = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psMember = conn.prepareStatement(memberSql)) {

            for (String[] sm : sampleMembers) {
                psUser.setString(1, sm[0]);
                psUser.setString(2, sm[1]);
                psUser.setString(3, sm[2]);
                psUser.setString(4, sm[3]);
                psUser.executeUpdate();

                int userId = -1;
                try (ResultSet rs = psUser.getGeneratedKeys()) {
                    if (rs.next()) userId = rs.getInt(1);
                }

                MemberType type = MemberType.valueOf(sm[3]);
                psMember.setString(1, sm[4]);
                psMember.setInt(2, userId);
                psMember.setString(3, type.name());
                psMember.setInt(4, type.getDefaultMaxLimit());
                psMember.setInt(5, type.getDefaultLoanDays());
                psMember.setInt(6, 0); // initial loans
                psMember.setString(7, "ACTIVE");
                psMember.executeUpdate();
            }
        }

        // 4. Seed initial loans (Active, Returned, and Overdue to showcase fines)
        LocalDate today = LocalDate.now();
        String loanSql = "INSERT INTO loans (book_id, member_id, issue_date, due_date, return_date, status, renewal_count, issued_by, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement psLoan = conn.prepareStatement(loanSql, Statement.RETURN_GENERATED_KEYS)) {
            // Loan 1: Active, Normal (Due in 10 days)
            psLoan.setInt(1, 1);
            psLoan.setString(2, "LIB-STU-1001");
            psLoan.setDate(3, java.sql.Date.valueOf(today.minusDays(4)));
            psLoan.setDate(4, java.sql.Date.valueOf(today.plusDays(10)));
            psLoan.setNull(5, java.sql.Types.DATE);
            psLoan.setString(6, "ACTIVE");
            psLoan.setInt(7, 0);
            psLoan.setString(8, "Head Librarian");
            psLoan.setString(9, "Standard Coursework");
            psLoan.executeUpdate();

            // Loan 2: Overdue (Issued 25 days ago, due 11 days ago)
            psLoan.setInt(1, 2);
            psLoan.setString(2, "LIB-STU-1002");
            psLoan.setDate(3, java.sql.Date.valueOf(today.minusDays(25)));
            psLoan.setDate(4, java.sql.Date.valueOf(today.minusDays(11)));
            psLoan.setNull(5, java.sql.Types.DATE);
            psLoan.setString(6, "OVERDUE");
            psLoan.setInt(7, 0);
            psLoan.setString(8, "Head Librarian");
            psLoan.setString(9, "Final Year Project Research");
            psLoan.executeUpdate();

            int overdueLoanId = -1;
            try (ResultSet rs = psLoan.getGeneratedKeys()) {
                if (rs.next()) overdueLoanId = rs.getInt(1);
            }

            // Loan 3: Returned in past
            psLoan.setInt(1, 3);
            psLoan.setString(2, "LIB-FAC-2001");
            psLoan.setDate(3, java.sql.Date.valueOf(today.minusDays(30)));
            psLoan.setDate(4, java.sql.Date.valueOf(today.minusDays(5)));
            psLoan.setDate(5, java.sql.Date.valueOf(today.minusDays(8)));
            psLoan.setString(6, "RETURNED");
            psLoan.setInt(7, 1);
            psLoan.setString(8, "Head Librarian");
            psLoan.setString(9, "Returned early in pristine condition");
            psLoan.executeUpdate();

            // Insert initial fine for the overdue loan
            if (overdueLoanId > 0) {
                String fineSql = "INSERT INTO fines (loan_id, member_id, overdue_days, fine_rate, total_fine, paid_amount, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psFine = conn.prepareStatement(fineSql)) {
                    psFine.setInt(1, overdueLoanId);
                    psFine.setString(2, "LIB-STU-1002");
                    psFine.setInt(3, 11);
                    psFine.setDouble(4, 1.00);
                    psFine.setDouble(5, 11.00);
                    psFine.setDouble(6, 0.00);
                    psFine.setString(7, "PENDING");
                    psFine.executeUpdate();
                }
            }
        }

        // Adjust book copy counts for books 1 and 2
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("UPDATE books SET available_copies = available_copies - 1 WHERE book_id IN (1, 2)");
            stmt.executeUpdate("UPDATE members SET active_loans_count = 1 WHERE member_id IN ('LIB-STU-1001', 'LIB-STU-1002')");
        }

        conn.commit();
        System.out.println("Seeding completed successfully! 1,000+ records generated.");
    }

    public static void main(String[] args) {
        try {
            boolean useSqlite = args.length > 0 && args[0].contains("sqlite");
            if (useSqlite) {
                com.library.config.AppConfig.set("db.type", "sqlite");
                com.library.config.AppConfig.set("db.url", "jdbc:sqlite:library.db");
            }
            System.out.println("Initializing database schema...");
            DBConnection.initializeDatabaseIfMissing();
            try (Connection conn = DBConnection.getInstance().getConnection()) {
                seedAll(conn, 1000);
            }
            System.out.println("Ready to run!");
        } catch (Exception e) {
            System.err.println("Seeder failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
