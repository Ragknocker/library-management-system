package com.library.model;

public enum MemberType {
    STUDENT("Student Card", 4, 14),
    FACULTY("Faculty Card", 10, 30),
    COMMUNITY("Community Patron", 2, 7);

    private final String displayName;
    private final int defaultMaxLimit;
    private final int defaultLoanDays;

    MemberType(String displayName, int defaultMaxLimit, int defaultLoanDays) {
        this.displayName = displayName;
        this.defaultMaxLimit = defaultMaxLimit;
        this.defaultLoanDays = defaultLoanDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultMaxLimit() {
        return defaultMaxLimit;
    }

    public int getDefaultLoanDays() {
        return defaultLoanDays;
    }
}
