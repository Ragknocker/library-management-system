package com.library.service;

import com.library.dao.IMemberDAO;
import com.library.dao.impl.MemberDAOImpl;
import com.library.exception.DatabaseException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.util.InputValidator;

import java.sql.SQLException;
import java.util.List;

/**
 * Service managing user accounts, member registration, and borrowing limits.
 */
public class MemberService {
    private final IMemberDAO memberDAO;

    public MemberService() {
        this.memberDAO = new MemberDAOImpl();
    }

    public MemberService(IMemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    public void registerMember(Member member) throws ValidationException, DatabaseException {
        InputValidator.validateMember(member);

        try {
            Member existingId = memberDAO.findById(member.getMemberId());
            if (existingId != null) {
                throw new ValidationException("Member Card ID '" + member.getMemberId() + "' is already registered to " + existingId.getName());
            }

            Member existingEmail = memberDAO.findByEmail(member.getEmail());
            if (existingEmail != null) {
                throw new ValidationException("Email '" + member.getEmail() + "' is already registered to member " + existingEmail.getMemberId());
            }

            memberDAO.insert(member);
        } catch (SQLException e) {
            throw new DatabaseException("Database error registering member: " + e.getMessage(), e);
        }
    }

    public void updateMember(Member member) throws ValidationException, DatabaseException, MemberNotFoundException {
        InputValidator.validateMember(member);

        try {
            Member existing = memberDAO.findById(member.getMemberId());
            if (existing == null) {
                throw new MemberNotFoundException("Member with ID " + member.getMemberId() + " not found.");
            }
            memberDAO.update(member);
        } catch (SQLException e) {
            throw new DatabaseException("Database error updating member: " + e.getMessage(), e);
        }
    }

    public void deleteMember(String memberId) throws DatabaseException, MemberNotFoundException {
        try {
            Member existing = memberDAO.findById(memberId);
            if (existing == null) {
                throw new MemberNotFoundException("Member with ID " + memberId + " not found.");
            }
            if (existing.getActiveLoansCount() > 0) {
                throw new DatabaseException("Cannot delete member. Member still has " +
                        existing.getActiveLoansCount() + " active loaned books to return.");
            }
            memberDAO.delete(memberId);
        } catch (SQLException e) {
            throw new DatabaseException("Database error deleting member: " + e.getMessage(), e);
        }
    }

    public Member getMemberById(String memberId) throws DatabaseException, MemberNotFoundException {
        try {
            Member member = memberDAO.findById(memberId);
            if (member == null) {
                throw new MemberNotFoundException("Member with ID '" + memberId + "' does not exist.");
            }
            return member;
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    public List<Member> searchMembers(String keyword, String type, int page, int pageSize) throws DatabaseException {
        try {
            int offset = Math.max(0, (page - 1) * pageSize);
            return memberDAO.search(keyword, type, offset, pageSize);
        } catch (SQLException e) {
            throw new DatabaseException("Database error during member search: " + e.getMessage(), e);
        }
    }

    public int countSearchResults(String keyword, String type) throws DatabaseException {
        try {
            return memberDAO.countSearch(keyword, type);
        } catch (SQLException e) {
            throw new DatabaseException("Database error counting members: " + e.getMessage(), e);
        }
    }
}
