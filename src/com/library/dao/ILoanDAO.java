package com.library.dao;

import com.library.model.LoanStatus;
import com.library.model.LoanTransaction;
import java.sql.Connection;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object Interface for Lending Loans Operations.
 */
public interface ILoanDAO {
    LoanTransaction findById(int loanId) throws SQLException;
    List<LoanTransaction> findActiveByMemberId(String memberId) throws SQLException;
    LoanTransaction findActiveByBookAndMember(int bookId, String memberId) throws SQLException;
    List<LoanTransaction> findAll(int offset, int limit) throws SQLException;
    List<LoanTransaction> findByStatus(LoanStatus status, int offset, int limit) throws SQLException;
    List<LoanTransaction> findOverdueLoans() throws SQLException;
    int countActiveLoans() throws SQLException;
    int countOverdueLoans() throws SQLException;

    // Transactional primitives with Connection passed in
    int issueBook(Connection conn, LoanTransaction loan) throws SQLException;
    boolean returnBook(Connection conn, int loanId, LocalDate returnDate) throws SQLException;
    boolean renewBook(int loanId, LocalDate newDueDate) throws SQLException;
}
