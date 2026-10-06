package com.library.util;

import com.library.exception.ValidationException;
import com.library.model.Book;
import com.library.model.Member;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Strict automated input validator for all domain entities and user inputs.
 * Enforces transactional consistency and eliminates runtime crashes.
 */
public class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[0-9\\-\\s()]{7,20}$"
    );

    // Stripped ISBN pattern (10 or 13 digits)
    private static final Pattern ISBN_CLEAN_PATTERN = Pattern.compile(
            "^(?:\\d{9}[\\dX]|\\d{13})$"
    );

    public static void validateBook(Book book) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (book == null) {
            throw new ValidationException("Book object cannot be null");
        }

        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            errors.add("Book Title is required and cannot be empty.");
        } else if (book.getTitle().trim().length() > 255) {
            errors.add("Book Title exceeds maximum length of 255 characters.");
        }

        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            errors.add("Author name is required and cannot be empty.");
        }

        if (book.getCategory() == null || book.getCategory().trim().isEmpty()) {
            errors.add("Category/Genre is required.");
        }

        // ISBN validation
        if (!isValidISBN(book.getIsbn())) {
            errors.add("Invalid ISBN format. Must be a valid 10-digit or 13-digit ISBN (e.g. 978-0-13-468599-1 or 0-13-468599-7).");
        }

        // Year validation
        int currentYear = Year.now().getValue();
        if (book.getPublicationYear() < 1450 || book.getPublicationYear() > currentYear + 1) {
            errors.add("Publication year must be between 1450 and " + (currentYear + 1) + ".");
        }

        // Copies validation
        if (book.getTotalCopies() <= 0) {
            errors.add("Total copies must be a positive integer (at least 1).");
        }
        if (book.getAvailableCopies() < 0) {
            errors.add("Available copies cannot be negative.");
        }
        if (book.getAvailableCopies() > book.getTotalCopies()) {
            errors.add("Available copies cannot exceed total copies.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static void validateMember(Member member) throws ValidationException {
        List<String> errors = new ArrayList<>();

        if (member == null) {
            throw new ValidationException("Member object cannot be null");
        }

        if (member.getMemberId() == null || member.getMemberId().trim().isEmpty()) {
            errors.add("Member Card ID is required (e.g. LIB-M-1001).");
        }

        if (member.getName() == null || member.getName().trim().isEmpty()) {
            errors.add("Member full name is required.");
        }

        if (!isValidEmail(member.getEmail())) {
            errors.add("Valid email address is required (e.g. name@university.edu).");
        }

        if (!isValidPhone(member.getPhone())) {
            errors.add("Valid contact phone number is required (at least 7 digits).");
        }

        if (member.getMemberType() == null) {
            errors.add("Member classification type (STUDENT, FACULTY, COMMUNITY) is required.");
        }

        if (member.getMaxBorrowLimit() <= 0) {
            errors.add("Max borrow limit must be at least 1.");
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public static boolean isValidISBN(String rawIsbn) {
        if (rawIsbn == null) return false;
        String clean = rawIsbn.replaceAll("[-\\s]", "").toUpperCase();
        if (!ISBN_CLEAN_PATTERN.matcher(clean).matches()) {
            return false;
        }

        if (clean.length() == 10) {
            return checkISBN10Checksum(clean);
        } else if (clean.length() == 13) {
            return checkISBN13Checksum(clean);
        }
        return false;
    }

    private static boolean checkISBN10Checksum(String clean) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            char c = clean.charAt(i);
            if (!Character.isDigit(c)) return false;
            sum += (c - '0') * (10 - i);
        }
        char last = clean.charAt(9);
        int lastVal = (last == 'X') ? 10 : (last - '0');
        sum += lastVal;
        return (sum % 11 == 0);
    }

    private static boolean checkISBN13Checksum(String clean) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = clean.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (clean.charAt(12) - '0');
    }

    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) return false;
        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static void validateNonEmpty(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " cannot be empty.");
        }
    }

    public static int validatePositiveInt(String value, String fieldName) throws ValidationException {
        try {
            int val = Integer.parseInt(value.trim());
            if (val <= 0) {
                throw new ValidationException(fieldName + " must be a positive integer greater than zero.");
            }
            return val;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid integer.");
        }
    }
}
