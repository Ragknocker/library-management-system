package com.library.ui;

import com.library.exception.LibraryException;
import com.library.model.*;
import com.library.service.BookService;
import com.library.service.LendingService;
import com.library.service.MemberService;
import com.library.ui.components.TableStyler;
import com.library.util.DateUtil;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Lending Desk Panel for checking out, returning, and renewing books.
 * Enforces ACID transactional integrity and concurrency isolation.
 */
public class LendingDeskPanel extends JPanel {
    private final LendingService lendingService;
    private final BookService bookService;
    private final MemberService memberService;
    private final MainFrame mainFrame;

    // Issue Desk components
    private JTextField txtIssueMemberId;
    private JLabel lblMemberPreview;
    private Member verifiedMember;

    private JTextField txtIssueBookIdOrIsbn;
    private JLabel lblBookPreview;
    private Book verifiedBook;

    private JLabel lblDueDatePreview;
    private JTextField txtIssueNotes;

    // Return Desk components
    private JTextField txtReturnMemberLookup;
    private DefaultTableModel returnLoansModel;
    private JTable returnLoansTable;
    private JLabel lblReturnFinePreview;
    private JCheckBox chkSettleFineNow;

    public LendingDeskPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.lendingService = new LendingService();
        this.bookService = new BookService();
        this.memberService = new MemberService();

        setLayout(new GridLayout(1, 2, 20, 0));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initIssueSection();
        initReturnSection();
    }

    private void initIssueSection() {
        JPanel issuePanel = new JPanel(new BorderLayout(0, 16));
        issuePanel.setBackground(UITheme.CARD_BG);
        issuePanel.setBorder(UITheme.createCardBorder());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("Lending Desk: Issue Book (ACID Checkout)");
        lblTitle.setFont(UITheme.FONT_HEADER);
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        header.add(lblTitle, BorderLayout.WEST);
        issuePanel.add(header, BorderLayout.NORTH);

        // Body form
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);

        // Member selection
        JLabel lblMem = new JLabel("1. Member Card ID:");
        lblMem.setFont(UITheme.FONT_BOLD);
        form.add(lblMem);
        form.add(Box.createVerticalStrut(4));

        JPanel memRow = new JPanel(new BorderLayout(8, 0));
        memRow.setOpaque(false);
        txtIssueMemberId = UITheme.createTextField();
        JButton btnCheckMember = UITheme.createSecondaryButton("Verify Patron");
        btnCheckMember.addActionListener(e -> verifyMember());
        memRow.add(txtIssueMemberId, BorderLayout.CENTER);
        memRow.add(btnCheckMember, BorderLayout.EAST);
        form.add(memRow);
        form.add(Box.createVerticalStrut(6));

        lblMemberPreview = new JLabel("No member verified yet.");
        lblMemberPreview.setFont(UITheme.FONT_SMALL);
        lblMemberPreview.setForeground(UITheme.TEXT_MUTED);
        form.add(lblMemberPreview);
        form.add(Box.createVerticalStrut(14));

        // Book selection
        JLabel lblBk = new JLabel("2. Book ID or ISBN:");
        lblBk.setFont(UITheme.FONT_BOLD);
        form.add(lblBk);
        form.add(Box.createVerticalStrut(4));

        JPanel bookRow = new JPanel(new BorderLayout(8, 0));
        bookRow.setOpaque(false);
        txtIssueBookIdOrIsbn = UITheme.createTextField();
        JButton btnCheckBook = UITheme.createSecondaryButton("Verify Book");
        btnCheckBook.addActionListener(e -> verifyBook());
        bookRow.add(txtIssueBookIdOrIsbn, BorderLayout.CENTER);
        bookRow.add(btnCheckBook, BorderLayout.EAST);
        form.add(bookRow);
        form.add(Box.createVerticalStrut(6));

        lblBookPreview = new JLabel("No book verified yet.");
        lblBookPreview.setFont(UITheme.FONT_SMALL);
        lblBookPreview.setForeground(UITheme.TEXT_MUTED);
        form.add(lblBookPreview);
        form.add(Box.createVerticalStrut(14));

        // Due date preview
        JLabel lblDueTitle = new JLabel("3. Calculated Due Date:");
        lblDueTitle.setFont(UITheme.FONT_BOLD);
        form.add(lblDueTitle);
        form.add(Box.createVerticalStrut(4));

        lblDueDatePreview = new JLabel("Calculates automatically on member verification.");
        lblDueDatePreview.setFont(UITheme.FONT_BODY);
        lblDueDatePreview.setForeground(UITheme.PRIMARY);
        form.add(lblDueDatePreview);
        form.add(Box.createVerticalStrut(14));

        // Notes
        JLabel lblNotes = new JLabel("4. Lending Notes / Coursework:");
        lblNotes.setFont(UITheme.FONT_BOLD);
        form.add(lblNotes);
        form.add(Box.createVerticalStrut(4));
        txtIssueNotes = UITheme.createTextField();
        form.add(txtIssueNotes);
        form.add(Box.createVerticalGlue());

        issuePanel.add(form, BorderLayout.CENTER);

        // Checkout Button
        JButton btnIssue = UITheme.createPrimaryButton("⚡ Complete Book Checkout");
        btnIssue.setFont(UITheme.FONT_HEADER);
        btnIssue.setPreferredSize(new Dimension(btnIssue.getWidth(), 42));
        btnIssue.addActionListener(e -> handleIssueBook());
        issuePanel.add(btnIssue, BorderLayout.SOUTH);

        add(issuePanel);
    }

    private void initReturnSection() {
        JPanel returnPanel = new JPanel(new BorderLayout(0, 14));
        returnPanel.setBackground(UITheme.CARD_BG);
        returnPanel.setBorder(UITheme.createCardBorder());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel lblTitle = new JLabel("Return Desk: Restock & Fine Settlement");
        lblTitle.setFont(UITheme.FONT_HEADER);
        lblTitle.setForeground(UITheme.TEXT_MAIN);
        header.add(lblTitle, BorderLayout.WEST);
        returnPanel.add(header, BorderLayout.NORTH);

        // Lookup bar
        JPanel lookupRow = new JPanel(new BorderLayout(8, 0));
        lookupRow.setOpaque(false);
        txtReturnMemberLookup = UITheme.createTextField();
        txtReturnMemberLookup.setToolTipText("Enter Member ID (e.g. LIB-STU-1001)");
        JButton btnFindLoans = UITheme.createPrimaryButton("Lookup Loans");
        btnFindLoans.addActionListener(e -> loadActiveLoansForMember());
        lookupRow.add(txtReturnMemberLookup, BorderLayout.CENTER);
        lookupRow.add(btnFindLoans, BorderLayout.EAST);

        // Table for active loans
        String[] cols = {"Loan ID", "Book Title", "Due Date", "Status"};
        returnLoansModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        returnLoansTable = new JTable(returnLoansModel);
        TableStyler.apply(returnLoansTable);
        returnLoansTable.getColumnModel().getColumn(0).setMaxWidth(70);
        returnLoansTable.getColumnModel().getColumn(3).setCellRenderer(TableStyler.createStatusRenderer());

        returnLoansTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateReturnFinePreview();
            }
        });

        JScrollPane scrollPane = new JScrollPane(returnLoansTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel centerBox = new JPanel(new BorderLayout(0, 8));
        centerBox.setOpaque(false);
        centerBox.add(lookupRow, BorderLayout.NORTH);
        centerBox.add(scrollPane, BorderLayout.CENTER);

        returnPanel.add(centerBox, BorderLayout.CENTER);

        // Bottom Actions & Fine Settlement
        JPanel bottomActionBox = new JPanel();
        bottomActionBox.setLayout(new BoxLayout(bottomActionBox, BoxLayout.Y_AXIS));
        bottomActionBox.setOpaque(false);

        lblReturnFinePreview = new JLabel("Select an active loan above to calculate return & fine status.");
        lblReturnFinePreview.setFont(UITheme.FONT_BODY);
        lblReturnFinePreview.setForeground(UITheme.TEXT_MAIN);

        chkSettleFineNow = new JCheckBox("Mark fine as Paid upon return (if any)");
        chkSettleFineNow.setFont(UITheme.FONT_BODY);
        chkSettleFineNow.setSelected(true);
        chkSettleFineNow.setOpaque(false);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 10, 0));
        btnRow.setOpaque(false);

        JButton btnRenew = UITheme.createSecondaryButton("↻ Renew Loan (+14d)");
        btnRenew.addActionListener(e -> handleRenewLoan());

        JButton btnReturn = UITheme.createPrimaryButton("⤶ Return & Restock");
        btnReturn.addActionListener(e -> handleReturnBook());

        btnRow.add(btnRenew);
        btnRow.add(btnReturn);

        bottomActionBox.add(lblReturnFinePreview);
        bottomActionBox.add(Box.createVerticalStrut(6));
        bottomActionBox.add(chkSettleFineNow);
        bottomActionBox.add(Box.createVerticalStrut(10));
        bottomActionBox.add(btnRow);

        returnPanel.add(bottomActionBox, BorderLayout.SOUTH);

        add(returnPanel);
    }

    private void verifyMember() {
        String mId = txtIssueMemberId.getText().trim();
        if (mId.isEmpty()) return;
        try {
            verifiedMember = memberService.getMemberById(mId);
            lblMemberPreview.setText(String.format("<html><b>%s</b> (%s)<br/>Active Loans: %d / %d allowed | Status: <font color='%s'>%s</font></html>",
                    verifiedMember.getName(), verifiedMember.getMemberType().getDisplayName(),
                    verifiedMember.getActiveLoansCount(), verifiedMember.getMaxBorrowLimit(),
                    "ACTIVE".equalsIgnoreCase(verifiedMember.getStatus()) ? "green" : "red",
                    verifiedMember.getStatus()));

            LocalDate dueDate = LocalDate.now().plusDays(verifiedMember.getBorrowDurationDays());
            lblDueDatePreview.setText(String.format("%s (%d loan days for %s)",
                    DateUtil.format(dueDate), verifiedMember.getBorrowDurationDays(), verifiedMember.getMemberType().getDisplayName()));
        } catch (Exception ex) {
            verifiedMember = null;
            lblMemberPreview.setText("<html><font color='red'>Member not found: " + ex.getMessage() + "</font></html>");
            lblDueDatePreview.setText("Calculates automatically on member verification.");
        }
    }

    private void verifyBook() {
        String query = txtIssueBookIdOrIsbn.getText().trim();
        if (query.isEmpty()) return;
        try {
            Book b = null;
            if (query.matches("\\d+")) {
                b = bookService.getBookById(Integer.parseInt(query));
            } else {
                b = bookService.getBookByIsbn(query);
            }

            if (b != null) {
                verifiedBook = b;
                lblBookPreview.setText(String.format("<html><b>%s</b> by %s<br/>Category: %s | Copies: <b>%d</b> available of %d total</html>",
                        b.getTitle(), b.getAuthor(), b.getCategory(), b.getAvailableCopies(), b.getTotalCopies()));
            } else {
                verifiedBook = null;
                lblBookPreview.setText("<html><font color='red'>Book not found with provided ID/ISBN.</font></html>");
            }
        } catch (Exception ex) {
            verifiedBook = null;
            lblBookPreview.setText("<html><font color='red'>Book lookup failed: " + ex.getMessage() + "</font></html>");
        }
    }

    private void handleIssueBook() {
        if (verifiedMember == null) {
            verifyMember();
            if (verifiedMember == null) {
                JOptionPane.showMessageDialog(this, "Please verify a valid member first.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        if (verifiedBook == null) {
            verifyBook();
            if (verifiedBook == null) {
                JOptionPane.showMessageDialog(this, "Please verify a valid book first.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        try {
            LoanTransaction loan = lendingService.issueBook(
                    verifiedBook.getBookId(),
                    verifiedMember.getMemberId(),
                    "System Librarian",
                    txtIssueNotes.getText().trim()
            );

            JOptionPane.showMessageDialog(this,
                    "Book successfully issued!\n" +
                    "• Loan ID: #" + loan.getLoanId() + "\n" +
                    "• Book: " + loan.getBookTitle() + "\n" +
                    "• Member: " + loan.getMemberName() + "\n" +
                    "• Due Date: " + DateUtil.format(loan.getDueDate()),
                    "Checkout Success (ACID)", JOptionPane.INFORMATION_MESSAGE);

            // Reset issue desk form
            txtIssueBookIdOrIsbn.setText("");
            txtIssueNotes.setText("");
            verifiedBook = null;
            lblBookPreview.setText("No book verified yet.");
            verifyMember(); // refresh member loan count

            mainFrame.getDashboardPanel().refreshData();
            mainFrame.getBookCatalogPanel().loadBooks();

        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Issue Rejected", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadActiveLoansForMember() {
        String mId = txtReturnMemberLookup.getText().trim();
        if (mId.isEmpty()) return;

        try {
            List<LoanTransaction> loans = lendingService.getActiveLoansByMember(mId);
            returnLoansModel.setRowCount(0);
            for (LoanTransaction lt : loans) {
                returnLoansModel.addRow(new Object[]{
                        lt.getLoanId(),
                        lt.getBookTitle(),
                        DateUtil.format(lt.getDueDate()),
                        lt.isCurrentlyOverdue() ? "OVERDUE" : "ACTIVE"
                });
            }
            if (loans.isEmpty()) {
                lblReturnFinePreview.setText("No active loans found for member " + mId);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateReturnFinePreview() {
        int row = returnLoansTable.getSelectedRow();
        if (row < 0) {
            lblReturnFinePreview.setText("Select an active loan above to calculate return & fine status.");
            return;
        }

        String dueDateStr = (String) returnLoansModel.getValueAt(row, 2);
        LocalDate dueDate = DateUtil.parse(dueDateStr);
        int overdueDays = DateUtil.calculateOverdueDays(dueDate, LocalDate.now());

        if (overdueDays > 0) {
            double fine = overdueDays * 1.00; // $1.00 per day
            lblReturnFinePreview.setText(String.format("<html><font color='red'><b>OVERDUE by %d days!</b> Fine: $%.2f ($1.00/day)</font></html>",
                    overdueDays, fine));
        } else {
            lblReturnFinePreview.setText("<html><font color='green'><b>On Time!</b> No fine incurred.</font></html>");
        }
    }

    private void handleReturnBook() {
        int row = returnLoansTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a loan from the table to return.", "Select Loan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int loanId = (int) returnLoansModel.getValueAt(row, 0);
        boolean settleNow = chkSettleFineNow.isSelected();

        try {
            FineRecord fine = lendingService.returnBook(loanId, LocalDate.now(), settleNow);
            String message = "Book returned and copy successfully restocked to catalog!";
            if (fine != null) {
                message += "\n• Overdue Penalty: $" + String.format("%.2f", fine.getTotalFine()) +
                           " (" + (settleNow ? "Paid in full" : "Pending collection") + ")";
            }
            JOptionPane.showMessageDialog(this, message, "Return Completed", JOptionPane.INFORMATION_MESSAGE);

            loadActiveLoansForMember();
            mainFrame.getDashboardPanel().refreshData();
            mainFrame.getBookCatalogPanel().loadBooks();

        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Return Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRenewLoan() {
        int row = returnLoansTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an active loan to renew.", "Select Loan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int loanId = (int) returnLoansModel.getValueAt(row, 0);
        try {
            lendingService.renewLoan(loanId);
            JOptionPane.showMessageDialog(this, "Loan #" + loanId + " successfully renewed for +14 days!", "Loan Renewed", JOptionPane.INFORMATION_MESSAGE);
            loadActiveLoansForMember();
            mainFrame.getDashboardPanel().refreshData();
        } catch (LibraryException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Renewal Rejected", JOptionPane.ERROR_MESSAGE);
        }
    }
}
