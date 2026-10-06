package com.library.model;

import java.time.LocalDateTime;

/**
 * Domain entity representing an overdue fine and its settlement status.
 */
public class FineRecord {
    private int fineId;
    private int loanId;
    private String memberId;
    private int overdueDays;
    private double fineRate;
    private double totalFine;
    private double paidAmount;
    private String status; // PENDING, PAID, WAIVED
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;

    // Display fields
    private String memberName;
    private String bookTitle;

    public FineRecord() {
        this.status = "PENDING";
        this.paidAmount = 0.00;
        this.createdAt = LocalDateTime.now();
    }

    public FineRecord(int fineId, int loanId, String memberId, int overdueDays,
                      double fineRate, double totalFine, String status) {
        this.fineId = fineId;
        this.loanId = loanId;
        this.memberId = memberId;
        this.overdueDays = overdueDays;
        this.fineRate = fineRate;
        this.totalFine = totalFine;
        this.paidAmount = 0.00;
        this.status = status != null ? status : "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public double getRemainingBalance() {
        return Math.max(0.00, totalFine - paidAmount);
    }

    // Getters and Setters
    public int getFineId() {
        return fineId;
    }

    public void setFineId(int fineId) {
        this.fineId = fineId;
    }

    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public int getOverdueDays() {
        return overdueDays;
    }

    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }

    public double getFineRate() {
        return fineRate;
    }

    public void setFineRate(double fineRate) {
        this.fineRate = fineRate;
    }

    public double getTotalFine() {
        return totalFine;
    }

    public void setTotalFine(double totalFine) {
        this.totalFine = totalFine;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }
}
