package com.library.util;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility functions for local dates, SQL dates, and overdue interval calculations.
 */
public class DateUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Date toSqlDate(LocalDate localDate) {
        return localDate != null ? Date.valueOf(localDate) : null;
    }

    public static LocalDate toLocalDate(Date sqlDate) {
        return sqlDate != null ? sqlDate.toLocalDate() : null;
    }

    public static String format(LocalDate localDate) {
        return localDate != null ? localDate.format(FORMATTER) : "N/A";
    }

    public static LocalDate parse(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) return null;
        return LocalDate.parse(dateStr.trim(), FORMATTER);
    }

    /**
     * Calculates days elapsed past due date. Returns 0 if not overdue.
     */
    public static int calculateOverdueDays(LocalDate dueDate, LocalDate returnDate) {
        if (dueDate == null) return 0;
        LocalDate referenceDate = (returnDate != null) ? returnDate : LocalDate.now();
        if (referenceDate.isAfter(dueDate)) {
            return (int) ChronoUnit.DAYS.between(dueDate, referenceDate);
        }
        return 0;
    }
}
