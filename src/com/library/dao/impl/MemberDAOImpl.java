package com.library.dao.impl;

import com.library.config.DBConnection;
import com.library.dao.IMemberDAO;
import com.library.model.Member;
import com.library.model.MemberType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC Implementation of Member DAO mapping Object-Oriented inheritance
 * to normalized relational tables (users + members).
 */
public class MemberDAOImpl implements IMemberDAO {

    @Override
    public Member findById(String memberId) throws SQLException {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, u.created_at, " +
                     "m.member_id, m.member_type, m.max_borrow_limit, m.borrow_duration_days, " +
                     "m.active_loans_count, m.status " +
                     "FROM users u JOIN members m ON u.user_id = m.user_id " +
                     "WHERE m.member_id = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memberId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public Member findByEmail(String email) throws SQLException {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, u.created_at, " +
                     "m.member_id, m.member_type, m.max_borrow_limit, m.borrow_duration_days, " +
                     "m.active_loans_count, m.status " +
                     "FROM users u JOIN members m ON u.user_id = m.user_id " +
                     "WHERE u.email = ?";
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<Member> findAll(int offset, int limit) throws SQLException {
        String sql = "SELECT u.user_id, u.name, u.email, u.phone, u.created_at, " +
                     "m.member_id, m.member_type, m.max_borrow_limit, m.borrow_duration_days, " +
                     "m.active_loans_count, m.status " +
                     "FROM users u JOIN members m ON u.user_id = m.user_id " +
                     "ORDER BY u.name ASC LIMIT ? OFFSET ?";
        List<Member> list = new ArrayList<>();
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
    public List<Member> search(String keyword, String memberType, int offset, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT u.user_id, u.name, u.email, u.phone, u.created_at, " +
                "m.member_id, m.member_type, m.max_borrow_limit, m.borrow_duration_days, " +
                "m.active_loans_count, m.status " +
                "FROM users u JOIN members m ON u.user_id = m.user_id WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (u.name LIKE ? OR u.email LIKE ? OR m.member_id LIKE ? OR u.phone LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (memberType != null && !memberType.trim().isEmpty() && !memberType.equalsIgnoreCase("ALL")) {
            sql.append("AND m.member_type = ? ");
            params.add(memberType.trim());
        }

        sql.append("ORDER BY u.name ASC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        List<Member> list = new ArrayList<>();
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    @Override
    public int countTotalMembers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM members";
        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    @Override
    public int countSearch(String keyword, String memberType) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM users u JOIN members m ON u.user_id = m.user_id WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (u.name LIKE ? OR u.email LIKE ? OR m.member_id LIKE ?) ");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }

        if (memberType != null && !memberType.trim().isEmpty() && !memberType.equalsIgnoreCase("ALL")) {
            sql.append("AND m.member_type = ? ");
            params.add(memberType.trim());
        }

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    @Override
    public boolean insert(Member member) throws SQLException {
        String userSql = "INSERT INTO users (name, email, phone, user_role) VALUES (?, ?, ?, ?)";
        String memberSql = "INSERT INTO members (member_id, user_id, member_type, max_borrow_limit, borrow_duration_days, active_loans_count, status) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false); // ACID Transaction

            int userId;
            try (PreparedStatement psUser = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, member.getName());
                psUser.setString(2, member.getEmail());
                psUser.setString(3, member.getPhone());
                psUser.setString(4, member.getMemberType().name());
                psUser.executeUpdate();

                try (ResultSet rsKeys = psUser.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        userId = rsKeys.getInt(1);
                        member.setUserId(userId);
                    } else {
                        throw new SQLException("Failed to retrieve generated user_id");
                    }
                }
            }

            try (PreparedStatement psMember = conn.prepareStatement(memberSql)) {
                psMember.setString(1, member.getMemberId());
                psMember.setInt(2, userId);
                psMember.setString(3, member.getMemberType().name());
                psMember.setInt(4, member.getMaxBorrowLimit());
                psMember.setInt(5, member.getBorrowDurationDays());
                psMember.setInt(6, member.getActiveLoansCount());
                psMember.setString(7, member.getStatus());
                psMember.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public boolean update(Member member) throws SQLException {
        String userSql = "UPDATE users SET name = ?, email = ?, phone = ? WHERE user_id = ?";
        String memberSql = "UPDATE members SET member_type = ?, max_borrow_limit = ?, borrow_duration_days = ?, status = ? WHERE member_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psUser = conn.prepareStatement(userSql)) {
                psUser.setString(1, member.getName());
                psUser.setString(2, member.getEmail());
                psUser.setString(3, member.getPhone());
                psUser.setInt(4, member.getUserId());
                psUser.executeUpdate();
            }

            try (PreparedStatement psMember = conn.prepareStatement(memberSql)) {
                psMember.setString(1, member.getMemberType().name());
                psMember.setInt(2, member.getMaxBorrowLimit());
                psMember.setInt(3, member.getBorrowDurationDays());
                psMember.setString(4, member.getStatus());
                psMember.setString(5, member.getMemberId());
                psMember.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public boolean delete(String memberId) throws SQLException {
        // Find user_id first to delete cascadingly from users
        String findUserSql = "SELECT user_id FROM members WHERE member_id = ?";
        String deleteUserSql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            int userId = -1;
            try (PreparedStatement psFind = conn.prepareStatement(findUserSql)) {
                psFind.setString(1, memberId);
                try (ResultSet rs = psFind.executeQuery()) {
                    if (rs.next()) {
                        userId = rs.getInt(1);
                    }
                }
            }

            if (userId != -1) {
                try (PreparedStatement psDel = conn.prepareStatement(deleteUserSql)) {
                    psDel.setInt(1, userId);
                    return psDel.executeUpdate() > 0;
                }
            }
            return false;
        }
    }

    @Override
    public boolean updateActiveLoansCount(Connection conn, String memberId, int delta) throws SQLException {
        String sql = "UPDATE members SET active_loans_count = active_loans_count + ? WHERE member_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setString(2, memberId);
            return ps.executeUpdate() > 0;
        }
    }

    private Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setUserId(rs.getInt("user_id"));
        m.setName(rs.getString("name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) m.setCreatedAt(ts.toLocalDateTime());

        m.setMemberId(rs.getString("member_id"));
        String typeStr = rs.getString("member_type");
        try {
            m.setMemberType(MemberType.valueOf(typeStr));
        } catch (Exception e) {
            m.setMemberType(MemberType.STUDENT);
        }
        m.setMaxBorrowLimit(rs.getInt("max_borrow_limit"));
        m.setBorrowDurationDays(rs.getInt("borrow_duration_days"));
        m.setActiveLoansCount(rs.getInt("active_loans_count"));
        m.setStatus(rs.getString("status"));
        return m;
    }
}
