package com.library.model;

import com.library.util.DateUtil;

import java.time.LocalDate;

/**
 * Entity representing an active or completed lending loan transaction.
 */
public class LoanTransaction {
    private int loanId;
    private int bookId;
    private String memberId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private LoanStatus status;
    private int renewalCount;
    private String issuedBy;
    private String notes;

    // View-helper fields to avoid redundant joins in UI tables
    private String bookTitle;
    private String bookIsbn;
    private String memberName;
    private String memberEmail;
    private int overdueDays;
    private double calculatedFine;

    public LoanTransaction() {
        this.status = LoanStatus.ACTIVE;
        this.renewalCount = 0;
        this.issuedBy = "Head Librarian";
    }

    public LoanTransaction(int loanId, int bookId, String memberId,
                           LocalDate issueDate, LocalDate dueDate,
                           LocalDate returnDate, LoanStatus status) {
        this.loanId = loanId;
        this.bookId = bookId;
        this.memberId = memberId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status != null ? status : LoanStatus.ACTIVE;
        this.renewalCount = 0;
        this.issuedBy = "Head Librarian";
    }

    public boolean isCurrentlyOverdue() {
        if (returnDate != null) {
            return returnDate.isAfter(dueDate);
        }
        return LocalDate.now().isAfter(dueDate);
    }

    public int computeOverdueDays() {
        return DateUtil.calculateOverdueDays(dueDate, returnDate);
    }

    // Getters and Setters
    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public int getRenewalCount() {
        return renewalCount;
    }

    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookIsbn() {
        return bookIsbn;
    }

    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public int getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }

    public double getCalculatedFine() {
        return calculatedFine;
    }

    public void setCalculatedFine(double calculatedFine) {
        this.calculatedFine = calculatedFine;
    }
}
