package com.library.dao.impl;

import com.library.config.DBConnection;
import com.library.dao.ILoanDAO;
import com.library.model.LoanStatus;
import com.library.model.LoanTransaction;
import com.library.util.DateUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Implementation of Loan DAO for lending workflows and audit history.
 */
public class LoanDAOImpl implements ILoanDAO {

    private static final String BASE_SELECT =
            "SELECT l.loan_id, l.book_id, l.member_id, l.issue_date, l.due_date, l.return_date, " +
            "l.status, l.renewal_count, l.issued_by, l.notes, " +
            "b.title AS book_title, b.isbn AS book_isbn, " +
            "u.name AS member_name, u.email AS member_email " +
            "FROM loans l " +
            "JOIN books b ON l.book_id = b.book_id " +
            "JOIN members m ON l.member_id = m.member_id " +
            "JOIN users u ON m.user_id = u.user_id ";

    @Override
    public LoanTransaction findById(int loanId) throws SQLException {
        String sql = BASE_SELECT + "WHERE l.loan_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<LoanTransaction> findActiveByMemberId(String memberId) throws SQLException {
        String sql = BASE_SELECT + "WHERE l.member_id = ? AND l.status = 'ACTIVE' ORDER BY l.due_date ASC";
        List<LoanTransaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public LoanTransaction findActiveByBookAndMember(int bookId, String memberId) throws SQLException {
        String sql = BASE_SELECT + "WHERE l.book_id = ? AND l.member_id = ? AND l.status = 'ACTIVE'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            ps.setString(2, memberId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<LoanTransaction> findAll(int offset, int limit) throws SQLException {
        String sql = BASE_SELECT + "ORDER BY l.loan_id DESC LIMIT ? OFFSET ?";
        List<LoanTransaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<LoanTransaction> findByStatus(LoanStatus status, int offset, int limit) throws SQLException {
        String sql = BASE_SELECT + "WHERE l.status = ? ORDER BY l.issue_date DESC LIMIT ? OFFSET ?";
        List<LoanTransaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public List<LoanTransaction> findOverdueLoans() throws SQLException {
        String sql = BASE_SELECT +
                     "WHERE (l.status = 'ACTIVE' AND l.due_date < CURRENT_DATE) " +
                     "OR l.status = 'OVERDUE' " +
                     "ORDER BY l.due_date ASC";
        List<LoanTransaction> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    @Override
    public int countActiveLoans() throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE status = 'ACTIVE'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    @Override
    public int countOverdueLoans() throws SQLException {
        String sql = "SELECT COUNT(*) FROM loans WHERE (status = 'ACTIVE' AND due_date < CURRENT_DATE) OR status = 'OVERDUE'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    @Override
    public int issueBook(Connection conn, LoanTransaction loan) throws SQLException {
        String sql = "INSERT INTO loans (book_id, member_id, issue_date, due_date, status, renewal_count, issued_by, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, loan.getBookId());
            ps.setString(2, loan.getMemberId());
            ps.setDate(3, DateUtil.toSqlDate(loan.getIssueDate()));
            ps.setDate(4, DateUtil.toSqlDate(loan.getDueDate()));
            ps.setString(5, loan.getStatus().name());
            ps.setInt(6, loan.getRenewalCount());
            ps.setString(7, loan.getIssuedBy());
            ps.setString(8, loan.getNotes());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        loan.setLoanId(id);
                        return id;
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public boolean returnBook(Connection conn, int loanId, LocalDate returnDate) throws SQLException {
        String sql = "UPDATE loans SET return_date = ?, status = 'RETURNED' WHERE loan_id = ? AND status != 'RETURNED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, DateUtil.toSqlDate(returnDate));
            ps.setInt(2, loanId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean renewBook(int loanId, LocalDate newDueDate) throws SQLException {
        String sql = "UPDATE loans SET due_date = ?, renewal_count = renewal_count + 1 WHERE loan_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, DateUtil.toSqlDate(newDueDate));
            ps.setInt(2, loanId);
            return ps.executeUpdate() > 0;
        }
    }

    private LoanTransaction mapRow(ResultSet rs) throws SQLException {
        LoanTransaction lt = new LoanTransaction();
        lt.setLoanId(rs.getInt("loan_id"));
        lt.setBookId(rs.getInt("book_id"));
        lt.setMemberId(rs.getString("member_id"));
        lt.setIssueDate(DateUtil.toLocalDate(rs.getDate("issue_date")));
        lt.setDueDate(DateUtil.toLocalDate(rs.getDate("due_date")));
        lt.setReturnDate(DateUtil.toLocalDate(rs.getDate("return_date")));

        String statusStr = rs.getString("status");
        try {
            lt.setStatus(LoanStatus.valueOf(statusStr));
        } catch (Exception e) {
            lt.setStatus(LoanStatus.ACTIVE);
        }

        lt.setRenewalCount(rs.getInt("renewal_count"));
        lt.setIssuedBy(rs.getString("issued_by"));
        lt.setNotes(rs.getString("notes"));

        lt.setBookTitle(rs.getString("book_title"));
        lt.setBookIsbn(rs.getString("book_isbn"));
        lt.setMemberName(rs.getString("member_name"));
        lt.setMemberEmail(rs.getString("member_email"));

        // Compute overdue days
        int overdueDays = lt.computeOverdueDays();
        lt.setOverdueDays(overdueDays);
        return lt;
    }
}
