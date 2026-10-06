package com.library.dao;

import com.library.model.FineRecord;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object Interface for Fines & Settlements.
 */
public interface IFineDAO {
    FineRecord findByLoanId(int loanId) throws SQLException;
    List<FineRecord> findByMemberId(String memberId) throws SQLException;
    List<FineRecord> findAllPending() throws SQLException;
    double calculateTotalPendingFines() throws SQLException;

    boolean recordFine(Connection conn, FineRecord fine) throws SQLException;
    boolean settleFine(int fineId, double amount, String status) throws SQLException;
}
