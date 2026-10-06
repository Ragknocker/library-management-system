package com.library.dao.impl;

import com.library.config.DBConnection;
import com.library.dao.IFineDAO;
import com.library.model.FineRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Implementation of Fine DAO for penalty collection and financial audit.
 */
public class FineDAOImpl implements IFineDAO {

    private static final String BASE_SELECT =
            "SELECT f.fine_id, f.loan_id, f.member_id, f.overdue_days, f.fine_rate, " +
            "f.total_fine, f.paid_amount, f.status, f.payment_date, f.created_at, " +
            "u.name AS member_name, b.title AS book_title " +
            "FROM fines f " +
            "JOIN members m ON f.member_id = m.member_id " +
            "JOIN users u ON m.user_id = u.user_id " +
            "JOIN loans l ON f.loan_id = l.loan_id " +
            "JOIN books b ON l.book_id = b.book_id ";

    @Override
    public FineRecord findByLoanId(int loanId) throws SQLException {
        String sql = BASE_SELECT + "WHERE f.loan_id = ?";
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
    public List<FineRecord> findByMemberId(String memberId) throws SQLException {
        String sql = BASE_SELECT + "WHERE f.member_id = ? ORDER BY f.fine_id DESC";
        List<FineRecord> list = new ArrayList<>();
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
    public List<FineRecord> findAllPending() throws SQLException {
        String sql = BASE_SELECT + "WHERE f.status = 'PENDING' ORDER BY f.fine_id DESC";
        List<FineRecord> list = new ArrayList<>();
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
    public double calculateTotalPendingFines() throws SQLException {
        String sql = "SELECT COALESCE(SUM(total_fine - paid_amount), 0.0) FROM fines WHERE status = 'PENDING'";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    @Override
    public boolean recordFine(Connection conn, FineRecord fine) throws SQLException {
        String sql = "INSERT INTO fines (loan_id, member_id, overdue_days, fine_rate, total_fine, paid_amount, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, fine.getLoanId());
            ps.setString(2, fine.getMemberId());
            ps.setInt(3, fine.getOverdueDays());
            ps.setDouble(4, fine.getFineRate());
            ps.setDouble(5, fine.getTotalFine());
            ps.setDouble(6, fine.getPaidAmount());
            ps.setString(7, fine.getStatus());

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        fine.setFineId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean settleFine(int fineId, double amount, String status) throws SQLException {
        String sql = "UPDATE fines SET paid_amount = paid_amount + ?, status = ?, payment_date = CURRENT_TIMESTAMP WHERE fine_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, status);
            ps.setInt(3, fineId);
            return ps.executeUpdate() > 0;
        }
    }

    private FineRecord mapRow(ResultSet rs) throws SQLException {
        FineRecord f = new FineRecord();
        f.setFineId(rs.getInt("fine_id"));
        f.setLoanId(rs.getInt("loan_id"));
        f.setMemberId(rs.getString("member_id"));
        f.setOverdueDays(rs.getInt("overdue_days"));
        f.setFineRate(rs.getDouble("fine_rate"));
        f.setTotalFine(rs.getDouble("total_fine"));
        f.setPaidAmount(rs.getDouble("paid_amount"));
        f.setStatus(rs.getString("status"));

        Timestamp paymentTs = rs.getTimestamp("payment_date");
        if (paymentTs != null) f.setPaymentDate(paymentTs.toLocalDateTime());

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) f.setCreatedAt(createdTs.toLocalDateTime());

        f.setMemberName(rs.getString("member_name"));
        f.setBookTitle(rs.getString("book_title"));
        return f;
    }
}
