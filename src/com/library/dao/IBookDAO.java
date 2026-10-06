package com.library.dao;

import com.library.model.Book;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data Access Object Interface for Book Catalog Operations.
 * Abstraction layer decoupling database interactions.
 */
public interface IBookDAO {
    Book findById(int id) throws SQLException;
    Book findByIsbn(String isbn) throws SQLException;
    List<Book> findAll(int offset, int limit) throws SQLException;
    List<Book> search(String keyword, String category, Boolean onlyAvailable, int offset, int limit) throws SQLException;
    int countSearch(String keyword, String category, Boolean onlyAvailable) throws SQLException;
    int countTotalBooks() throws SQLException;
    int countTotalCopies() throws SQLException;
    int countAvailableCopies() throws SQLException;
    List<String> getAllCategories() throws SQLException;

    boolean insert(Book book) throws SQLException;
    boolean update(Book book) throws SQLException;
    boolean delete(int id) throws SQLException;

    // Transactional primitives used within ACID transactions
    boolean decrementAvailableCopies(Connection conn, int bookId) throws SQLException;
    boolean incrementAvailableCopies(Connection conn, int bookId) throws SQLException;
}
