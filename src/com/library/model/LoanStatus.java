package com.library.model;

public enum LoanStatus {
    ACTIVE("Active / On Loan"),
    RETURNED("Returned"),
    OVERDUE("Overdue");

    private final String label;

    LoanStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
