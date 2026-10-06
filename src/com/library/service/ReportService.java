package com.library.service;

import com.library.config.DBConnection;
import com.library.dao.IBookDAO;
import com.library.dao.IFineDAO;
import com.library.dao.ILoanDAO;
import com.library.dao.IMemberDAO;
import com.library.dao.impl.BookDAOImpl;
import com.library.dao.impl.FineDAOImpl;
import com.library.dao.impl.LoanDAOImpl;
import com.library.dao.impl.MemberDAOImpl;
import com.library.exception.DatabaseException;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Service aggregating system-wide metrics and inventory analytics.
 */
public class ReportService {
    private final IBookDAO bookDAO = new BookDAOImpl();
    private final IMemberDAO memberDAO = new MemberDAOImpl();
    private final ILoanDAO loanDAO = new LoanDAOImpl();
    private final IFineDAO fineDAO = new FineDAOImpl();

    public static class SystemStats {
        public int totalBooks;
        public int totalCopies;
        public int availableCopies;
        public int issuedCopies;
        public int totalMembers;
        public int activeLoans;
        public int overdueLoans;
        public double pendingFinesAmount;
    }

    public SystemStats getSystemStats() throws DatabaseException {
        try {
            SystemStats stats = new SystemStats();
            stats.totalBooks = bookDAO.countTotalBooks();
            stats.totalCopies = bookDAO.countTotalCopies();
            stats.availableCopies = bookDAO.countAvailableCopies();
            stats.issuedCopies = Math.max(0, stats.totalCopies - stats.availableCopies);
            stats.totalMembers = memberDAO.countTotalMembers();
            stats.activeLoans = loanDAO.countActiveLoans();
            stats.overdueLoans = loanDAO.countOverdueLoans();
            stats.pendingFinesAmount = fineDAO.calculateTotalPendingFines();
            return stats;
        } catch (SQLException e) {
            throw new DatabaseException("Error gathering system analytics: " + e.getMessage(), e);
        }
    }

    public Map<String, Integer> getBooksPerCategory() throws DatabaseException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT category, COUNT(*) AS cnt FROM books GROUP BY category ORDER BY cnt DESC";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString("category"), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error gathering category distribution: " + e.getMessage(), e);
        }
        return map;
    }
}
