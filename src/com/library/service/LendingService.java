package com.library.service;

import com.library.config.AppConfig;
import com.library.config.DBConnection;
import com.library.dao.IBookDAO;
import com.library.dao.IFineDAO;
import com.library.dao.ILoanDAO;
import com.library.dao.IMemberDAO;
import com.library.dao.impl.BookDAOImpl;
import com.library.dao.impl.FineDAOImpl;
import com.library.dao.impl.LoanDAOImpl;
import com.library.dao.impl.MemberDAOImpl;
import com.library.exception.*;
import com.library.model.*;
import com.library.util.DateUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Core Circulation and Lending Engine.
 * Enforces ACID compliance, transactional boundaries,
 * and eliminates concurrency conflicts using atomic database updates.
 */
public class LendingService {
    private final IBookDAO bookDAO;
    private final IMemberDAO memberDAO;
    private final ILoanDAO loanDAO;
    private final IFineDAO fineDAO;

    public LendingService() {
        this.bookDAO = new BookDAOImpl();
        this.memberDAO = new MemberDAOImpl();
        this.loanDAO = new LoanDAOImpl();
        this.fineDAO = new FineDAOImpl();
    }

    public LendingService(IBookDAO bookDAO, IMemberDAO memberDAO, ILoanDAO loanDAO, IFineDAO fineDAO) {
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
        this.loanDAO = loanDAO;
        this.fineDAO = fineDAO;
    }

    /**
     * Issues a book to a registered member with ACID guarantees and concurrency protection.
     *
     * @return The newly generated LoanTransaction record.
     */
    public LoanTransaction issueBook(int bookId, String memberId, String issuedBy, String notes)
            throws LibraryException {
        // Step 1: Input Validation
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new ValidationException("Member ID is required to issue a book.");
        }
        if (bookId <= 0) {
            throw new ValidationException("Invalid Book ID.");
        }

        try {
            // Step 2: Validate Member Status and Borrowing Limits
            Member member = memberDAO.findById(memberId.trim());
            if (member == null) {
                throw new MemberNotFoundException("Member with ID '" + memberId + "' does not exist.");
            }
            if (!"ACTIVE".equalsIgnoreCase(member.getStatus())) {
                throw new ValidationException("Member account is " + member.getStatus() + ". Cannot issue books.");
            }
            if (member.getActiveLoansCount() >= member.getMaxBorrowLimit()) {
                throw new MemberLimitExceededException(String.format(
                        "Member %s has reached the maximum borrowing limit of %d books (%s limit).",
                        member.getName(), member.getMaxBorrowLimit(), member.getMemberType().getDisplayName()
                ));
            }

            // Step 3: Prevent duplicate loans of the same book to the same member
            LoanTransaction duplicate = loanDAO.findActiveByBookAndMember(bookId, memberId);
            if (duplicate != null) {
                throw new ActiveLoanExistsException("Member already has an active loan for this book (Loan ID #" + duplicate.getLoanId() + ").");
            }

            // Step 4: Validate Book existence
            Book book = bookDAO.findById(bookId);
            if (book == null) {
                throw new BookNotFoundException("Book with ID " + bookId + " does not exist.");
            }
            if (book.getAvailableCopies() <= 0) {
                throw new BookUnavailableException("All copies of '" + book.getTitle() + "' are currently checked out.");
            }

            // Step 5: Execute Transaction with ACID Isolation
            LocalDate issueDate = LocalDate.now();
            LocalDate dueDate = issueDate.plusDays(member.getBorrowDurationDays());

            LoanTransaction loan = new LoanTransaction();
            loan.setBookId(bookId);
            loan.setMemberId(member.getMemberId());
            loan.setIssueDate(issueDate);
            loan.setDueDate(dueDate);
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setIssuedBy(issuedBy != null && !issuedBy.trim().isEmpty() ? issuedBy : "Librarian");
            loan.setNotes(notes);

            Connection conn = null;
            try {
                conn = DBConnection.getInstance().getConnection();
                conn.setAutoCommit(false); // Begin ACID Transaction
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                // Atomic Concurrency Elimination: Decrement copies only if available > 0
                boolean copyDeducted = bookDAO.decrementAvailableCopies(conn, bookId);
                if (!copyDeducted) {
                    conn.rollback();
                    throw new BookUnavailableException("Concurrency Conflict: Book copy was claimed by another checkout request.");
                }

                // Increment member active loan count
                boolean memberUpdated = memberDAO.updateActiveLoansCount(conn, member.getMemberId(), 1);
                if (!memberUpdated) {
                    conn.rollback();
                    throw new DatabaseException("Failed to update member loan balance.");
                }

                // Insert loan record
                int loanId = loanDAO.issueBook(conn, loan);
                if (loanId <= 0) {
                    conn.rollback();
                    throw new DatabaseException("Failed to generate loan transaction record.");
                }

                // Commit Transaction
                conn.commit();

                loan.setLoanId(loanId);
                loan.setBookTitle(book.getTitle());
                loan.setMemberName(member.getName());
                return loan;

            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                }
                throw new DatabaseException("ACID Transaction Failed during book checkout: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Processes book return, calculates any overdue fines,
     * and atomically updates inventory with ACID compliance.
     */
    public FineRecord returnBook(int loanId, LocalDate returnDate, boolean settleFineNow)
            throws LibraryException {
        if (loanId <= 0) {
            throw new ValidationException("Invalid Loan ID.");
        }
        if (returnDate == null) {
            returnDate = LocalDate.now();
        }

        try {
            LoanTransaction loan = loanDAO.findById(loanId);
            if (loan == null) {
                throw new ValidationException("Loan record #" + loanId + " not found.");
            }
            if (loan.getStatus() == LoanStatus.RETURNED) {
                throw new ValidationException("Loan #" + loanId + " has already been returned on " + DateUtil.format(loan.getReturnDate()));
            }

            // Calculate overdue days and fine
            int overdueDays = DateUtil.calculateOverdueDays(loan.getDueDate(), returnDate);
            double dailyRate = AppConfig.getDouble("policy.fine.per.day", 1.00);
            double totalFine = overdueDays * dailyRate;

            Connection conn = null;
            FineRecord fine = null;

            try {
                conn = DBConnection.getInstance().getConnection();
                conn.setAutoCommit(false); // Begin ACID Transaction

                // Step A: Mark loan returned
                boolean loanUpdated = loanDAO.returnBook(conn, loanId, returnDate);
                if (!loanUpdated) {
                    conn.rollback();
                    throw new DatabaseException("Failed to update loan status to RETURNED.");
                }

                // Step B: Increment available copies atomically
                boolean bookRestocked = bookDAO.incrementAvailableCopies(conn, loan.getBookId());
                if (!bookRestocked) {
                    conn.rollback();
                    throw new DatabaseException("Failed to increment available copies in catalog.");
                }

                // Step C: Decrement member active loans count
                boolean memberUpdated = memberDAO.updateActiveLoansCount(conn, loan.getMemberId(), -1);
                if (!memberUpdated) {
                    conn.rollback();
                    throw new DatabaseException("Failed to decrement member loan count.");
                }

                // Step D: Create Fine record if overdue
                if (overdueDays > 0 && totalFine > 0) {
                    fine = new FineRecord();
                    fine.setLoanId(loanId);
                    fine.setMemberId(loan.getMemberId());
                    fine.setOverdueDays(overdueDays);
                    fine.setFineRate(dailyRate);
                    fine.setTotalFine(totalFine);
                    fine.setPaidAmount(settleFineNow ? totalFine : 0.00);
                    fine.setStatus(settleFineNow ? "PAID" : "PENDING");

                    boolean fineRecorded = fineDAO.recordFine(conn, fine);
                    if (!fineRecorded) {
                        conn.rollback();
                        throw new DatabaseException("Failed to register overdue fine record.");
                    }
                }

                // Commit Transaction
                conn.commit();
                return fine;

            } catch (SQLException e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                }
                throw new DatabaseException("ACID Transaction Failed during book return: " + e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
                }
            }

        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Renews an active loan if not overdue and within renewal threshold.
     */
    public boolean renewLoan(int loanId) throws LibraryException {
        try {
            LoanTransaction loan = loanDAO.findById(loanId);
            if (loan == null) {
                throw new ValidationException("Loan record #" + loanId + " not found.");
            }
            if (loan.getStatus() != LoanStatus.ACTIVE) {
                throw new ValidationException("Only active loans can be renewed.");
            }
            if (loan.isCurrentlyOverdue()) {
                throw new ValidationException("Overdue loans cannot be renewed. Please return the book and clear any fines.");
            }
            if (loan.getRenewalCount() >= 2) {
                throw new ValidationException("Maximum renewal limit (2 times) reached for this loan.");
            }

            LocalDate newDueDate = loan.getDueDate().plusDays(14);
            return loanDAO.renewBook(loanId, newDueDate);

        } catch (SQLException e) {
            throw new DatabaseException("Database error during renewal: " + e.getMessage(), e);
        }
    }

    /**
     * Settles a fine by paying the balance.
     */
    public void settleFine(int fineId, double amount) throws LibraryException {
        if (amount <= 0) {
            throw new ValidationException("Payment amount must be greater than zero.");
        }
        try {
            fineDAO.settleFine(fineId, amount, "PAID");
        } catch (SQLException e) {
            throw new DatabaseException("Database error settling fine: " + e.getMessage(), e);
        }
    }

    public List<LoanTransaction> getActiveLoansByMember(String memberId) throws DatabaseException {
        try {
            return loanDAO.findActiveByMemberId(memberId);
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving member loans: " + e.getMessage(), e);
        }
    }

    public List<LoanTransaction> getOverdueLoans() throws DatabaseException {
        try {
            return loanDAO.findOverdueLoans();
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving overdue loans: " + e.getMessage(), e);
        }
    }

    public List<LoanTransaction> getAllLoans(int page, int pageSize) throws DatabaseException {
        try {
            int offset = Math.max(0, (page - 1) * pageSize);
            return loanDAO.findAll(offset, pageSize);
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving loan history: " + e.getMessage(), e);
        }
    }

    public List<FineRecord> getPendingFines() throws DatabaseException {
        try {
            return fineDAO.findAllPending();
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving pending fines: " + e.getMessage(), e);
        }
    }
}
