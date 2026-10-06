package com.library.dao;

import com.library.model.Member;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object Interface for Member Accounts Operations.
 */
public interface IMemberDAO {
    Member findById(String memberId) throws SQLException;
    Member findByEmail(String email) throws SQLException;
    List<Member> findAll(int offset, int limit) throws SQLException;
    List<Member> search(String keyword, String memberType, int offset, int limit) throws SQLException;
    int countTotalMembers() throws SQLException;
    int countSearch(String keyword, String memberType) throws SQLException;

    boolean insert(Member member) throws SQLException;
    boolean update(Member member) throws SQLException;
    boolean delete(String memberId) throws SQLException;

    // Transactional primitives
    boolean updateActiveLoansCount(Connection conn, String memberId, int delta) throws SQLException;
}
