package com.library.service;

import com.library.dao.IBookDAO;
import com.library.dao.impl.BookDAOImpl;
import com.library.exception.BookNotFoundException;
import com.library.exception.DatabaseException;
import com.library.exception.ValidationException;
import com.library.model.Book;
import com.library.util.InputValidator;

import java.sql.SQLException;
import java.util.List;

/**
 * Service orchestrating book cataloging, automated validation, and inventory searches.
 */
public class BookService {
    private final IBookDAO bookDAO;

    public BookService() {
        this.bookDAO = new BookDAOImpl();
    }

    public BookService(IBookDAO bookDAO) {
        this.bookDAO = bookDAO;
    }

    public void registerNewBook(Book book) throws ValidationException, DatabaseException {
        // Strict input validation
        InputValidator.validateBook(book);

        try {
            // Check for ISBN duplicate
            Book existing = bookDAO.findByIsbn(book.getIsbn());
            if (existing != null) {
                throw new ValidationException("A book with ISBN '" + book.getIsbn() + "' already exists in catalog: '" + existing.getTitle() + "'");
            }
            bookDAO.insert(book);
        } catch (SQLException e) {
            throw new DatabaseException("Database error while registering book: " + e.getMessage(), e);
        }
    }

    public void updateBook(Book book) throws ValidationException, DatabaseException, BookNotFoundException {
        InputValidator.validateBook(book);

        try {
            Book existing = bookDAO.findById(book.getBookId());
            if (existing == null) {
                throw new BookNotFoundException("Book with ID " + book.getBookId() + " not found.");
            }
            bookDAO.update(book);
        } catch (SQLException e) {
            throw new DatabaseException("Database error while updating book: " + e.getMessage(), e);
        }
    }

    public void deleteBook(int bookId) throws DatabaseException, BookNotFoundException {
        try {
            Book existing = bookDAO.findById(bookId);
            if (existing == null) {
                throw new BookNotFoundException("Book with ID " + bookId + " not found.");
            }
            if (existing.getIssuedCopies() > 0) {
                throw new DatabaseException("Cannot delete book '" + existing.getTitle() + "'. " +
                        existing.getIssuedCopies() + " copies are currently issued to members.");
            }
            bookDAO.delete(bookId);
        } catch (SQLException e) {
            throw new DatabaseException("Database error while deleting book: " + e.getMessage(), e);
        }
    }

    public Book getBookById(int bookId) throws DatabaseException, BookNotFoundException {
        try {
            Book book = bookDAO.findById(bookId);
            if (book == null) {
                throw new BookNotFoundException("Book with ID " + bookId + " not found.");
            }
            return book;
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    public Book getBookByIsbn(String isbn) throws DatabaseException {
        try {
            return bookDAO.findByIsbn(isbn);
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    public List<Book> searchBooks(String keyword, String category, Boolean onlyAvailable, int page, int pageSize) throws DatabaseException {
        try {
            int offset = Math.max(0, (page - 1) * pageSize);
            return bookDAO.search(keyword, category, onlyAvailable, offset, pageSize);
        } catch (SQLException e) {
            throw new DatabaseException("Database error during book search: " + e.getMessage(), e);
        }
    }

    public int countSearchResults(String keyword, String category, Boolean onlyAvailable) throws DatabaseException {
        try {
            return bookDAO.countSearch(keyword, category, onlyAvailable);
        } catch (SQLException e) {
            throw new DatabaseException("Database error counting books: " + e.getMessage(), e);
        }
    }

    public List<String> getAllCategories() throws DatabaseException {
        try {
            return bookDAO.getAllCategories();
        } catch (SQLException e) {
            throw new DatabaseException("Database error fetching categories: " + e.getMessage(), e);
        }
    }
}
