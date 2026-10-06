package com.library.ui;

import com.library.exception.BookNotFoundException;
import com.library.exception.DatabaseException;
import com.library.exception.ValidationException;
import com.library.model.Book;
import com.library.service.BookService;
import com.library.ui.components.TableStyler;
import com.library.util.InputValidator;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Catalog Panel supporting live search, filtering, pagination,
 * and CRUD operations across 1,000+ inventory records.
 */
public class BookCatalogPanel extends JPanel {
    private final BookService bookService;
    private final MainFrame mainFrame;

    private JTextField txtSearch;
    private JComboBox<String> cmbCategory;
    private JCheckBox chkOnlyAvailable;
    private DefaultTableModel tableModel;
    private JTable bookTable;

    private JLabel lblPaginationInfo;
    private JButton btnPrevPage;
    private JButton btnNextPage;
    private int currentPage = 1;
    private final int pageSize = 25;
    private int totalRecords = 0;

    public BookCatalogPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.bookService = new BookService();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initUI();
    }

    private void initUI() {
        // Top Toolbar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Book Inventory & Catalog Management");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("+ Add New Book");
        btnAdd.addActionListener(e -> showAddBookDialog());

        JButton btnEdit = UITheme.createSecondaryButton("✎ Edit Selected");
        btnEdit.addActionListener(e -> showEditBookDialog());

        JButton btnDelete = UITheme.createDangerButton("✕ Delete");
        btnDelete.addActionListener(e -> handleDeleteBook());

        actionBtns.add(btnAdd);
        actionBtns.add(btnEdit);
        actionBtns.add(btnDelete);

        headerBar.add(lblTitle, BorderLayout.WEST);
        headerBar.add(actionBtns, BorderLayout.EAST);
        topContainer.add(headerBar);
        topContainer.add(Box.createVerticalStrut(14));

        // Filter / Search Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterBar.setBackground(UITheme.CARD_BG);
        filterBar.setBorder(UITheme.createCardBorder());

        JLabel lblSearch = new JLabel("Search:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        txtSearch = UITheme.createTextField();
        txtSearch.setPreferredSize(new Dimension(220, 32));
        txtSearch.setToolTipText("Search title, author, or ISBN");
        txtSearch.addActionListener(e -> { currentPage = 1; loadBooks(); });

        JLabel lblCat = new JLabel("Category:");
        lblCat.setFont(UITheme.FONT_BOLD);
        cmbCategory = new JComboBox<>(new String[]{"ALL"});
        cmbCategory.setFont(UITheme.FONT_BODY);
        cmbCategory.setPreferredSize(new Dimension(180, 32));
        cmbCategory.addActionListener(e -> { currentPage = 1; loadBooks(); });

        chkOnlyAvailable = new JCheckBox("Available Only");
        chkOnlyAvailable.setFont(UITheme.FONT_BODY);
        chkOnlyAvailable.setOpaque(false);
        chkOnlyAvailable.addActionListener(e -> { currentPage = 1; loadBooks(); });

        JButton btnFilter = UITheme.createPrimaryButton("Search");
        btnFilter.addActionListener(e -> { currentPage = 1; loadBooks(); });

        JButton btnClear = UITheme.createSecondaryButton("Reset");
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbCategory.setSelectedIndex(0);
            chkOnlyAvailable.setSelected(false);
            currentPage = 1;
            loadBooks();
        });

        filterBar.add(lblSearch);
        filterBar.add(txtSearch);
        filterBar.add(lblCat);
        filterBar.add(cmbCategory);
        filterBar.add(chkOnlyAvailable);
        filterBar.add(btnFilter);
        filterBar.add(btnClear);

        topContainer.add(filterBar);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setBackground(UITheme.CARD_BG);
        tableContainer.setBorder(UITheme.createCardBorder());

        String[] cols = {"ID", "ISBN", "Title", "Author", "Category", "Year", "Total", "Available", "Location", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        bookTable = new JTable(tableModel);
        TableStyler.apply(bookTable);
        bookTable.getColumnModel().getColumn(0).setMaxWidth(60);
        bookTable.getColumnModel().getColumn(5).setMaxWidth(70);
        bookTable.getColumnModel().getColumn(6).setMaxWidth(60);
        bookTable.getColumnModel().getColumn(7).setMaxWidth(70);
        bookTable.getColumnModel().getColumn(9).setCellRenderer(TableStyler.createStatusRenderer());

        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        // Pagination Bar
        JPanel paginationBar = new JPanel(new BorderLayout());
        paginationBar.setOpaque(false);
        paginationBar.setBorder(new EmptyBorder(8, 4, 4, 4));

        lblPaginationInfo = new JLabel("Showing 0 to 0 of 0 books");
        lblPaginationInfo.setFont(UITheme.FONT_SUBTITLE);
        lblPaginationInfo.setForeground(UITheme.TEXT_MUTED);

        JPanel pBtnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pBtnBox.setOpaque(false);

        btnPrevPage = UITheme.createSecondaryButton("◀ Previous");
        btnPrevPage.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadBooks();
            }
        });

        btnNextPage = UITheme.createSecondaryButton("Next ▶");
        btnNextPage.addActionListener(e -> {
            int maxPages = (int) Math.ceil((double) totalRecords / pageSize);
            if (currentPage < maxPages) {
                currentPage++;
                loadBooks();
            }
        });

        pBtnBox.add(btnPrevPage);
        pBtnBox.add(btnNextPage);

        paginationBar.add(lblPaginationInfo, BorderLayout.WEST);
        paginationBar.add(pBtnBox, BorderLayout.EAST);
        tableContainer.add(paginationBar, BorderLayout.SOUTH);

        add(tableContainer, BorderLayout.CENTER);
    }

    public void loadCategories() {
        try {
            List<String> categories = bookService.getAllCategories();
            cmbCategory.removeAllItems();
            cmbCategory.addItem("ALL");
            for (String c : categories) {
                cmbCategory.addItem(c);
            }
        } catch (Exception ignored) {}
    }

    public void loadBooks() {
        String keyword = txtSearch.getText().trim();
        String cat = (String) cmbCategory.getSelectedItem();
        boolean onlyAvail = chkOnlyAvailable.isSelected();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Book> books;
            int count = 0;

            @Override
            protected Void doInBackground() throws Exception {
                count = bookService.countSearchResults(keyword, cat, onlyAvail);
                books = bookService.searchBooks(keyword, cat, onlyAvail, currentPage, pageSize);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    totalRecords = count;
                    tableModel.setRowCount(0);
                    if (books != null) {
                        for (Book b : books) {
                            String statusStr = b.getAvailableCopies() > 0
                                    ? "In Stock (" + b.getAvailableCopies() + ")"
                                    : "Out of Stock";
                            tableModel.addRow(new Object[]{
                                    b.getBookId(),
                                    b.getIsbn(),
                                    b.getTitle(),
                                    b.getAuthor(),
                                    b.getCategory(),
                                    b.getPublicationYear(),
                                    b.getTotalCopies(),
                                    b.getAvailableCopies(),
                                    b.getShelfLocation(),
                                    statusStr
                            });
                        }
                    }

                    int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / pageSize));
                    int startIdx = totalRecords == 0 ? 0 : (currentPage - 1) * pageSize + 1;
                    int endIdx = Math.min(currentPage * pageSize, totalRecords);

                    lblPaginationInfo.setText(String.format("Showing %d to %d of %,d books (Page %d of %d)",
                            startIdx, endIdx, totalRecords, currentPage, totalPages));

                    btnPrevPage.setEnabled(currentPage > 1);
                    btnNextPage.setEnabled(currentPage < totalPages);

                } catch (Exception e) {
                    System.err.println("Failed to load books: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showAddBookDialog() {
        JDialog dialog = new JDialog(mainFrame, "Add New Book to Catalog", true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(8, 2, 10, 10));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField tfIsbn = UITheme.createTextField();
        JTextField tfTitle = UITheme.createTextField();
        JTextField tfAuthor = UITheme.createTextField();
        JTextField tfPublisher = UITheme.createTextField();
        JTextField tfYear = UITheme.createTextField();
        tfYear.setText(String.valueOf(java.time.Year.now().getValue()));
        JTextField tfCategory = UITheme.createTextField();
        JTextField tfTotalCopies = UITheme.createTextField();
        tfTotalCopies.setText("3");
        JTextField tfShelf = UITheme.createTextField();
        tfShelf.setText("Stack A-01");

        form.add(new JLabel("ISBN (10 or 13 digits):*")); form.add(tfIsbn);
        form.add(new JLabel("Book Title:*")); form.add(tfTitle);
        form.add(new JLabel("Author Name:*")); form.add(tfAuthor);
        form.add(new JLabel("Publisher:")); form.add(tfPublisher);
        form.add(new JLabel("Publication Year:*")); form.add(tfYear);
        form.add(new JLabel("Category / Genre:*")); form.add(tfCategory);
        form.add(new JLabel("Total Copies:*")); form.add(tfTotalCopies);
        form.add(new JLabel("Shelf Location:")); form.add(tfShelf);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = UITheme.createPrimaryButton("Save Book");
        btnSave.addActionListener(e -> {
            try {
                Book b = new Book();
                b.setIsbn(tfIsbn.getText().trim());
                b.setTitle(tfTitle.getText().trim());
                b.setAuthor(tfAuthor.getText().trim());
                b.setPublisher(tfPublisher.getText().trim());
                b.setPublicationYear(InputValidator.validatePositiveInt(tfYear.getText(), "Publication Year"));
                b.setCategory(tfCategory.getText().trim());
                int totalCopies = InputValidator.validatePositiveInt(tfTotalCopies.getText(), "Total Copies");
                b.setTotalCopies(totalCopies);
                b.setAvailableCopies(totalCopies);
                b.setShelfLocation(tfShelf.getText().trim());

                bookService.registerNewBook(b);
                JOptionPane.showMessageDialog(dialog, "Book '" + b.getTitle() + "' successfully cataloged!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadCategories();
                loadBooks();
                mainFrame.getDashboardPanel().refreshData();
            } catch (ValidationException | DatabaseException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showEditBookDialog() {
        int row = bookTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a book from the table to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookId = (int) tableModel.getValueAt(row, 0);
        try {
            Book b = bookService.getBookById(bookId);

            JDialog dialog = new JDialog(mainFrame, "Edit Book: " + b.getTitle(), true);
            dialog.setSize(520, 480);
            dialog.setLocationRelativeTo(mainFrame);
            dialog.setLayout(new BorderLayout());

            JPanel form = new GridLayout(8, 2, 10, 10) != null ? new JPanel(new GridLayout(8, 2, 10, 10)) : new JPanel();
            form.setBorder(new EmptyBorder(20, 20, 20, 20));

            JTextField tfIsbn = UITheme.createTextField();
            tfIsbn.setText(b.getIsbn());
            tfIsbn.setEditable(false); // ISBN is unique key
            JTextField tfTitle = UITheme.createTextField();
            tfTitle.setText(b.getTitle());
            JTextField tfAuthor = UITheme.createTextField();
            tfAuthor.setText(b.getAuthor());
            JTextField tfPublisher = UITheme.createTextField();
            tfPublisher.setText(b.getPublisher());
            JTextField tfYear = UITheme.createTextField();
            tfYear.setText(String.valueOf(b.getPublicationYear()));
            JTextField tfCategory = UITheme.createTextField();
            tfCategory.setText(b.getCategory());
            JTextField tfTotalCopies = UITheme.createTextField();
            tfTotalCopies.setText(String.valueOf(b.getTotalCopies()));
            JTextField tfShelf = UITheme.createTextField();
            tfShelf.setText(b.getShelfLocation());

            form.add(new JLabel("ISBN (Immutable):")); form.add(tfIsbn);
            form.add(new JLabel("Book Title:*")); form.add(tfTitle);
            form.add(new JLabel("Author Name:*")); form.add(tfAuthor);
            form.add(new JLabel("Publisher:")); form.add(tfPublisher);
            form.add(new JLabel("Publication Year:*")); form.add(tfYear);
            form.add(new JLabel("Category / Genre:*")); form.add(tfCategory);
            form.add(new JLabel("Total Copies:*")); form.add(tfTotalCopies);
            form.add(new JLabel("Shelf Location:")); form.add(tfShelf);

            dialog.add(form, BorderLayout.CENTER);

            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
            JButton btnCancel = UITheme.createSecondaryButton("Cancel");
            btnCancel.addActionListener(e -> dialog.dispose());

            JButton btnUpdate = UITheme.createPrimaryButton("Update Details");
            btnUpdate.addActionListener(e -> {
                try {
                    b.setTitle(tfTitle.getText().trim());
                    b.setAuthor(tfAuthor.getText().trim());
                    b.setPublisher(tfPublisher.getText().trim());
                    b.setPublicationYear(InputValidator.validatePositiveInt(tfYear.getText(), "Publication Year"));
                    b.setCategory(tfCategory.getText().trim());
                    int newTotal = InputValidator.validatePositiveInt(tfTotalCopies.getText(), "Total Copies");
                    int diff = newTotal - b.getTotalCopies();
                    b.setTotalCopies(newTotal);
                    b.setAvailableCopies(Math.max(0, b.getAvailableCopies() + diff));
                    b.setShelfLocation(tfShelf.getText().trim());

                    bookService.updateBook(b);
                    JOptionPane.showMessageDialog(dialog, "Book updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadBooks();
                    mainFrame.getDashboardPanel().refreshData();
                } catch (ValidationException | DatabaseException | BookNotFoundException ex) {
                    JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            btnBar.add(btnCancel);
            btnBar.add(btnUpdate);
            dialog.add(btnBar, BorderLayout.SOUTH);

            dialog.setVisible(true);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteBook() {
        int row = bookTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a book to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bookId = (int) tableModel.getValueAt(row, 0);
        String title = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete book #" + bookId + " - '" + title + "'?\nThis action cannot be undone.",
                "Confirm Book Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                bookService.deleteBook(bookId);
                JOptionPane.showMessageDialog(this, "Book successfully removed from catalog.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                loadBooks();
                mainFrame.getDashboardPanel().refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Deletion Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
