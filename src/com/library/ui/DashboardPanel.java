package com.library.ui;

import com.library.exception.DatabaseException;
import com.library.model.LoanTransaction;
import com.library.service.LendingService;
import com.library.service.ReportService;
import com.library.ui.components.StatCard;
import com.library.ui.components.TableStyler;
import com.library.util.DateUtil;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Dashboard Panel displaying live metrics, quick operations, and recent lending activity.
 */
public class DashboardPanel extends JPanel {
    private final ReportService reportService;
    private final LendingService lendingService;
    private final MainFrame mainFrame;

    private StatCard cardTotalBooks;
    private StatCard cardAvailableCopies;
    private StatCard cardActiveLoans;
    private StatCard cardOverdueFines;

    private DefaultTableModel recentLoansModel;
    private JTable recentLoansTable;

    public DashboardPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.reportService = new ReportService();
        this.lendingService = new LendingService();

        setLayout(new BorderLayout(16, 20));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initUI();
    }

    private void initUI() {
        // Top Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Library Analytics & Operations Dashboard");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JButton btnRefresh = UITheme.createSecondaryButton("↻ Refresh Stats");
        btnRefresh.addActionListener(e -> refreshData());

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(btnRefresh, BorderLayout.EAST);

        // Stats Cards Grid
        JPanel statsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        statsGrid.setOpaque(false);

        cardTotalBooks = new StatCard("Catalog Inventory", "0", "Total unique titles", UITheme.PRIMARY);
        cardAvailableCopies = new StatCard("Available Copies", "0", "In stock for checkout", UITheme.SUCCESS);
        cardActiveLoans = new StatCard("Active Borrowers", "0", "Currently loaned out", UITheme.WARNING);
        cardOverdueFines = new StatCard("Overdue & Fines", "0", "$0.00 pending fines", UITheme.DANGER);

        statsGrid.add(cardTotalBooks);
        statsGrid.add(cardAvailableCopies);
        statsGrid.add(cardActiveLoans);
        statsGrid.add(cardOverdueFines);

        // Quick Operations Bar
        JPanel quickBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        quickBar.setBackground(UITheme.CARD_BG);
        quickBar.setBorder(UITheme.createCardBorder());

        JLabel lblQuick = new JLabel("Quick Actions:");
        lblQuick.setFont(UITheme.FONT_BOLD);
        lblQuick.setForeground(UITheme.TEXT_MAIN);
        quickBar.add(lblQuick);

        JButton btnQuickIssue = UITheme.createPrimaryButton("➔ Issue Book");
        btnQuickIssue.addActionListener(e -> mainFrame.switchToTab(2)); // Lending tab
        quickBar.add(btnQuickIssue);

        JButton btnQuickReturn = UITheme.createSecondaryButton("⤶ Return Book");
        btnQuickReturn.addActionListener(e -> mainFrame.switchToTab(2));
        quickBar.add(btnQuickReturn);

        JButton btnAddBook = UITheme.createSecondaryButton("+ Add New Book");
        btnAddBook.addActionListener(e -> mainFrame.switchToTab(1)); // Books tab
        quickBar.add(btnAddBook);

        JButton btnAddMember = UITheme.createSecondaryButton("+ Register Member");
        btnAddMember.addActionListener(e -> mainFrame.switchToTab(3)); // Members tab
        quickBar.add(btnAddMember);

        // North container combining header, cards, and quick bar
        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);
        northContainer.add(headerPanel);
        northContainer.add(Box.createVerticalStrut(16));
        northContainer.add(statsGrid);
        northContainer.add(Box.createVerticalStrut(16));
        northContainer.add(quickBar);

        add(northContainer, BorderLayout.NORTH);

        // Center: Recent Activity Table
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setBackground(UITheme.CARD_BG);
        tableContainer.setBorder(UITheme.createCardBorder());

        JLabel lblTableTitle = new JLabel("Recent Lending Activity & Transactions");
        lblTableTitle.setFont(UITheme.FONT_HEADER);
        lblTableTitle.setForeground(UITheme.TEXT_MAIN);
        tableContainer.add(lblTableTitle, BorderLayout.NORTH);

        String[] cols = {"Loan ID", "Book Title", "ISBN", "Member Name", "Issue Date", "Due Date", "Status"};
        recentLoansModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        recentLoansTable = new JTable(recentLoansModel);
        TableStyler.apply(recentLoansTable);
        recentLoansTable.getColumnModel().getColumn(6).setCellRenderer(TableStyler.createStatusRenderer());
        recentLoansTable.getColumnModel().getColumn(0).setMaxWidth(80);

        JScrollPane scrollPane = new JScrollPane(recentLoansTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    public void refreshData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            ReportService.SystemStats stats;
            List<LoanTransaction> recentLoans;

            @Override
            protected Void doInBackground() throws Exception {
                stats = reportService.getSystemStats();
                recentLoans = lendingService.getAllLoans(1, 15);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (stats != null) {
                        cardTotalBooks.setValue(String.format("%,d", stats.totalBooks));
                        cardTotalBooks.setSubtitle(String.format("%,d total physical copies", stats.totalCopies));

                        cardAvailableCopies.setValue(String.format("%,d", stats.availableCopies));
                        cardAvailableCopies.setSubtitle(String.format("%,d copies currently issued", stats.issuedCopies));

                        cardActiveLoans.setValue(String.format("%,d", stats.activeLoans));
                        cardActiveLoans.setSubtitle(String.format("%d registered patrons", stats.totalMembers));

                        cardOverdueFines.setValue(String.valueOf(stats.overdueLoans));
                        cardOverdueFines.setSubtitle(String.format("$%.2f pending fines", stats.pendingFinesAmount));
                    }

                    recentLoansModel.setRowCount(0);
                    if (recentLoans != null) {
                        for (LoanTransaction lt : recentLoans) {
                            recentLoansModel.addRow(new Object[]{
                                    lt.getLoanId(),
                                    lt.getBookTitle(),
                                    lt.getBookIsbn(),
                                    lt.getMemberName(),
                                    DateUtil.format(lt.getIssueDate()),
                                    DateUtil.format(lt.getDueDate()),
                                    lt.getStatus().name()
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Dashboard refresh error: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }
}
