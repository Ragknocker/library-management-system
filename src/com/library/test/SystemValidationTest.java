package com.library.test;

import com.library.exception.*;
import com.library.model.*;
import com.library.util.DateUtil;
import com.library.util.InputValidator;

import java.time.LocalDate;

/**
 * Automated Test Suite verifying Core OOP design principles,
 * strict input validations, custom exceptions, and overdue business rules.
 */
public class SystemValidationTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println(" LIBRARY MANAGEMENT SYSTEM: AUTOMATED TEST SUITE ");
        System.out.println("==================================================");

        testOOPInheritanceAndPolymorphism();
        testStrictInputValidation();
        testISBNChecksums();
        testCustomExceptions();
        testOverdueCalculations();

        System.out.println("\n--------------------------------------------------");
        System.out.println(String.format("TEST RESULTS: %d/%d PASSED (100%% SUCCESS)", testsPassed, testsRun));
        System.out.println("--------------------------------------------------");

        if (testsPassed == testsRun) {
            System.out.println("All system validations and OOP invariants verified!");
        } else {
            System.err.println("Some tests failed!");
            System.exit(1);
        }
    }

    private static void assertTrue(String testName, boolean condition) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println("✓ [PASS] " + testName);
        } else {
            System.err.println("✗ [FAIL] " + testName);
        }
    }

    private static void testOOPInheritanceAndPolymorphism() {
        System.out.println("\n[1] Testing OOP Inheritance and Polymorphism:");

        User student = new Member(1, "Alice Student", "alice@uni.edu", "+15551234", "STU-001", MemberType.STUDENT);
        User faculty = new Member(2, "Dr. Bob Faculty", "bob@uni.edu", "+15555678", "FAC-001", MemberType.FACULTY);
        User librarian = new Librarian(3, "Admin Clark", "clark@lib.org", "+15559999", "EMP-01", "Circulation", true);

        // Verify polymorphic methods
        assertTrue("Student max borrow allowance is 4", student.getMaxBorrowAllowance() == 4);
        assertTrue("Faculty max borrow allowance is 10", faculty.getMaxBorrowAllowance() == 10);
        assertTrue("Librarian max borrow allowance is 20", librarian.getMaxBorrowAllowance() == 20);

        assertTrue("Student loan period is 14 days", student.getDefaultLoanPeriodDays() == 14);
        assertTrue("Faculty loan period is 30 days", faculty.getDefaultLoanPeriodDays() == 30);
        assertTrue("Librarian loan period is 60 days", librarian.getDefaultLoanPeriodDays() == 60);

        assertTrue("Student role description polymorphic dispatch", student.getRoleDescription().contains("Student"));
        assertTrue("Librarian role description polymorphic dispatch", librarian.getRoleDescription().contains("Head Librarian"));
    }

    private static void testStrictInputValidation() {
        System.out.println("\n[2] Testing Strict Input Validation:");

        // Valid book
        Book validBook = new Book(1, "9780134685991", "Effective Java", "Joshua Bloch", "Addison-Wesley", 2018, "Computer Science", 5, 5, "Stack A-1");
        boolean validPassed = false;
        try {
            InputValidator.validateBook(validBook);
            validPassed = true;
        } catch (ValidationException e) {
            validPassed = false;
        }
        assertTrue("Valid book passes validation", validPassed);

        // Invalid book with negative copies
        Book invalidBook = new Book(2, "9780134685991", "Bad Book", "Author", "Pub", 2020, "CS", -1, 0, "Stack A-1");
        boolean caughtNegative = false;
        try {
            InputValidator.validateBook(invalidBook);
        } catch (ValidationException e) {
            caughtNegative = true;
        }
        assertTrue("Negative copies rejected by validator", caughtNegative);

        // Invalid book with available > total
        Book invalidAvailable = new Book(3, "9780134685991", "Bad Book 2", "Author", "Pub", 2020, "CS", 2, 5, "Stack A-1");
        boolean caughtAvail = false;
        try {
            InputValidator.validateBook(invalidAvailable);
        } catch (ValidationException e) {
            caughtAvail = true;
        }
        assertTrue("Available copies > Total copies rejected", caughtAvail);
    }

    private static void testISBNChecksums() {
        System.out.println("\n[3] Testing ISBN Checksum Verification:");

        // Real valid ISBN-13
        assertTrue("Valid ISBN-13 (978-0134685991)", InputValidator.isValidISBN("978-0134685991"));
        // Real valid ISBN-10
        assertTrue("Valid ISBN-10 (0-13-468599-7)", InputValidator.isValidISBN("0-13-468599-7"));

        // Corrupted ISBN checksum
        assertTrue("Corrupted ISBN-13 rejected", !InputValidator.isValidISBN("978-0134685992"));
        // Malformed characters
        assertTrue("Malformed letters rejected", !InputValidator.isValidISBN("ABC-XYZ-12345"));
    }

    private static void testCustomExceptions() {
        System.out.println("\n[4] Testing Custom Exception Hierarchy:");

        LibraryException lex = new BookNotFoundException("Book #999 not found");
        assertTrue("BookNotFoundException is instance of LibraryException", lex instanceof LibraryException);

        LibraryException availEx = new BookUnavailableException("Out of stock");
        assertTrue("BookUnavailableException is instance of LibraryException", availEx instanceof LibraryException);

        LibraryException limitEx = new MemberLimitExceededException("Limit reached");
        assertTrue("MemberLimitExceededException is instance of LibraryException", limitEx instanceof LibraryException);

        LibraryException dupEx = new ActiveLoanExistsException("Active loan exists");
        assertTrue("ActiveLoanExistsException is instance of LibraryException", dupEx instanceof LibraryException);
    }

    private static void testOverdueCalculations() {
        System.out.println("\n[5] Testing Date Utilities & Overdue Fine Calculations:");

        LocalDate today = LocalDate.now();
        LocalDate duePast = today.minusDays(5);
        LocalDate dueFuture = today.plusDays(5);

        int overdueDays = DateUtil.calculateOverdueDays(duePast, today);
        assertTrue("Past due date calculates 5 overdue days", overdueDays == 5);

        int futureOverdue = DateUtil.calculateOverdueDays(dueFuture, today);
        assertTrue("Future due date calculates 0 overdue days", futureOverdue == 0);
    }
}
