package com.library.ui;

import com.library.exception.DatabaseException;
import com.library.exception.LibraryException;
import com.library.model.FineRecord;
import com.library.model.LoanTransaction;
import com.library.service.LendingService;
import com.library.ui.components.TableStyler;
import com.library.util.DateUtil;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Panel managing overdue borrowing violations, fine calculations, and payments.
 */
public class OverdueFinesPanel extends JPanel {
    private final LendingService lendingService;
    private final MainFrame mainFrame;

    private DefaultTableModel overdueTableModel;
    private JTable overdueTable;
    private DefaultTableModel finesTableModel;
    private JTable finesTable;

    public OverdueFinesPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.lendingService = new LendingService();

        setLayout(new GridLayout(2, 1, 0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initUI();
    }

    private void initUI() {
        // Upper Section: Active Overdue Loans
        JPanel overduePanel = new JPanel(new BorderLayout(0, 10));
        overduePanel.setBackground(UITheme.CARD_BG);
        overduePanel.setBorder(UITheme.createCardBorder());

        JPanel headerOverdue = new JPanel(new BorderLayout());
        headerOverdue.setOpaque(false);
        JLabel lblTitle1 = new JLabel("Currently Overdue Loans (Action Required)");
        lblTitle1.setFont(UITheme.FONT_HEADER);
        lblTitle1.setForeground(UITheme.DANGER);

        JButton btnRefresh = UITheme.createSecondaryButton("↻ Refresh All");
        btnRefresh.addActionListener(e -> loadData());

        headerOverdue.add(lblTitle1, BorderLayout.WEST);
        headerOverdue.add(btnRefresh, BorderLayout.EAST);
        overduePanel.add(headerOverdue, BorderLayout.NORTH);

        String[] colsOverdue = {"Loan ID", "Book Title", "Member ID", "Member Name", "Due Date", "Days Overdue", "Estimated Fine ($)"};
        overdueTableModel = new DefaultTableModel(colsOverdue, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        overdueTable = new JTable(overdueTableModel);
        TableStyler.apply(overdueTable);
        overdueTable.getColumnModel().getColumn(0).setMaxWidth(70);
        overdueTable.getColumnModel().getColumn(5).setMaxWidth(100);
        overdueTable.getColumnModel().getColumn(6).setMaxWidth(130);

        JScrollPane scrollOverdue = new JScrollPane(overdueTable);
        scrollOverdue.setBorder(BorderFactory.createEmptyBorder());
        scrollOverdue.getViewport().setBackground(Color.WHITE);
        overduePanel.add(scrollOverdue, BorderLayout.CENTER);

        add(overduePanel);

        // Lower Section: Outstanding Fines & Settlement
        JPanel finesPanel = new JPanel(new BorderLayout(0, 10));
        finesPanel.setBackground(UITheme.CARD_BG);
        finesPanel.setBorder(UITheme.createCardBorder());

        JPanel headerFines = new JPanel(new BorderLayout());
        headerFines.setOpaque(false);
        JLabel lblTitle2 = new JLabel("Unpaid Fine Records (Pending Settlements)");
        lblTitle2.setFont(UITheme.FONT_HEADER);
        lblTitle2.setForeground(UITheme.TEXT_MAIN);

        JButton btnSettle = UITheme.createPrimaryButton("$ Settle Selected Fine");
        btnSettle.addActionListener(e -> handleSettleFine());

        headerFines.add(lblTitle2, BorderLayout.WEST);
        headerFines.add(btnSettle, BorderLayout.EAST);
        finesPanel.add(headerFines, BorderLayout.NORTH);

        String[] colsFines = {"Fine ID", "Loan ID", "Member ID", "Member Name", "Book Title", "Overdue Days", "Total Fine", "Paid", "Balance", "Status"};
        finesTableModel = new DefaultTableModel(colsFines, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        finesTable = new JTable(finesTableModel);
        TableStyler.apply(finesTable);
        finesTable.getColumnModel().getColumn(9).setCellRenderer(TableStyler.createStatusRenderer());

        JScrollPane scrollFines = new JScrollPane(finesTable);
        scrollFines.setBorder(BorderFactory.createEmptyBorder());
        scrollFines.getViewport().setBackground(Color.WHITE);
        finesPanel.add(scrollFines, BorderLayout.CENTER);

        add(finesPanel);
    }

    public void loadData() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<LoanTransaction> overdues;
            List<FineRecord> pendingFines;

            @Override
            protected Void doInBackground() throws Exception {
                overdues = lendingService.getOverdueLoans();
                pendingFines = lendingService.getPendingFines();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    overdueTableModel.setRowCount(0);
                    if (overdues != null) {
                        for (LoanTransaction lt : overdues) {
                            int days = lt.getOverdueDays();
                            double estFine = days * 1.00;
                            overdueTableModel.addRow(new Object[]{
                                    lt.getLoanId(),
                                    lt.getBookTitle(),
                                    lt.getMemberId(),
                                    lt.getMemberName(),
                                    DateUtil.format(lt.getDueDate()),
                                    days + " days",
                                    String.format("$%.2f", estFine)
                            });
                        }
                    }

                    finesTableModel.setRowCount(0);
                    if (pendingFines != null) {
                        for (FineRecord f : pendingFines) {
                            finesTableModel.addRow(new Object[]{
                                    f.getFineId(),
                                    f.getLoanId(),
                                    f.getMemberId(),
                                    f.getMemberName(),
                                    f.getBookTitle(),
                                    f.getOverdueDays(),
                                    String.format("$%.2f", f.getTotalFine()),
                                    String.format("$%.2f", f.getPaidAmount()),
                                    String.format("$%.2f", f.getRemainingBalance()),
                                    f.getStatus()
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed loading overdue/fine data: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void handleSettleFine() {
        int row = finesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an unpaid fine from the table to settle.", "Select Fine", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int fineId = (int) finesTableModel.getValueAt(row, 0);
        String memberName = (String) finesTableModel.getValueAt(row, 3);
        String balanceStr = ((String) finesTableModel.getValueAt(row, 8)).replace("$", "");
        double balance = Double.parseDouble(balanceStr);

        String input = JOptionPane.showInputDialog(this,
                "Settle fine for " + memberName + "\nRemaining balance: $" + String.format("%.2f", balance) + "\nEnter payment amount:",
                String.format("%.2f", balance));

        if (input != null && !input.trim().isEmpty()) {
            try {
                double amount = Double.parseDouble(input.trim());
                lendingService.settleFine(fineId, amount);
                JOptionPane.showMessageDialog(this, "Payment of $" + String.format("%.2f", amount) + " recorded successfully!",
                        "Fine Settled", JOptionPane.INFORMATION_MESSAGE);
                loadData();
                mainFrame.getDashboardPanel().refreshData();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Please enter a valid monetary number.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            } catch (LibraryException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
