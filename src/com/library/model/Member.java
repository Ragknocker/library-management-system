package com.library.model;

/**
 * Concrete entity representing a Library Member (Card Holder).
 * Inherits from User and overrides polymorphic methods.
 */
public class Member extends User {
    private String memberId;
    private MemberType memberType;
    private int maxBorrowLimit;
    private int borrowDurationDays;
    private int activeLoansCount;
    private String status; // ACTIVE, SUSPENDED, EXPIRED

    public Member() {
        super();
        this.memberType = MemberType.STUDENT;
        this.maxBorrowLimit = MemberType.STUDENT.getDefaultMaxLimit();
        this.borrowDurationDays = MemberType.STUDENT.getDefaultLoanDays();
        this.status = "ACTIVE";
    }

    public Member(int userId, String name, String email, String phone,
                  String memberId, MemberType memberType) {
        super(userId, name, email, phone);
        this.memberId = memberId;
        this.memberType = memberType != null ? memberType : MemberType.STUDENT;
        this.maxBorrowLimit = this.memberType.getDefaultMaxLimit();
        this.borrowDurationDays = this.memberType.getDefaultLoanDays();
        this.activeLoansCount = 0;
        this.status = "ACTIVE";
    }

    // Polymorphic implementations
    @Override
    public String getRoleDescription() {
        return "Member (" + memberType.getDisplayName() + ")";
    }

    @Override
    public int getMaxBorrowAllowance() {
        return maxBorrowLimit;
    }

    @Override
    public int getDefaultLoanPeriodDays() {
        return borrowDurationDays;
    }

    public boolean canBorrowMore() {
        return "ACTIVE".equalsIgnoreCase(status) && (activeLoansCount < maxBorrowLimit);
    }

    // Getters and Setters
    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public MemberType getMemberType() {
        return memberType;
    }

    public void setMemberType(MemberType memberType) {
        this.memberType = memberType;
    }

    public int getMaxBorrowLimit() {
        return maxBorrowLimit;
    }

    public void setMaxBorrowLimit(int maxBorrowLimit) {
        this.maxBorrowLimit = maxBorrowLimit;
    }

    public int getBorrowDurationDays() {
        return borrowDurationDays;
    }

    public void setBorrowDurationDays(int borrowDurationDays) {
        this.borrowDurationDays = borrowDurationDays;
    }

    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    public void setActiveLoansCount(int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
