package com.library.ui;

import com.library.model.LoanTransaction;
import com.library.service.LendingService;
import com.library.service.ReportService;
import com.library.ui.components.TableStyler;
import com.library.util.DateUtil;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Reports and Audit Log Panel for system analytics and export capabilities.
 */
public class ReportsPanel extends JPanel {
    private final ReportService reportService;
    private final LendingService lendingService;
    private final MainFrame mainFrame;

    private DefaultTableModel categoryTableModel;
    private DefaultTableModel auditTableModel;
    private JTable auditTable;

    public ReportsPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.reportService = new ReportService();
        this.lendingService = new LendingService();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initUI();
    }

    private void initUI() {
        // Upper Toolbar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Library System Reports & Audit Trail");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnBox.setOpaque(false);

        JButton btnExport = UITheme.createPrimaryButton("⤓ Export Audit to CSV");
        btnExport.addActionListener(e -> exportAuditToCSV());

        JButton btnRefresh = UITheme.createSecondaryButton("↻ Refresh Reports");
        btnRefresh.addActionListener(e -> loadReports());

        btnBox.add(btnExport);
        btnBox.add(btnRefresh);

        topBar.add(lblTitle, BorderLayout.WEST);
        topBar.add(btnBox, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Center Split: Left Category Breakdown, Right Full Audit Log
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(360);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);

        // Left Panel: Category Breakdown
        JPanel leftPanel = new JPanel(new BorderLayout(0, 10));
        leftPanel.setBackground(UITheme.CARD_BG);
        leftPanel.setBorder(UITheme.createCardBorder());

        JLabel lblCatTitle = new JLabel("Inventory by Subject/Category");
        lblCatTitle.setFont(UITheme.FONT_HEADER);
        leftPanel.add(lblCatTitle, BorderLayout.NORTH);

        String[] catCols = {"Category", "Titles"};
        categoryTableModel = new DefaultTableModel(catCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable catTable = new JTable(categoryTableModel);
        TableStyler.apply(catTable);
        catTable.getColumnModel().getColumn(1).setMaxWidth(80);

        JScrollPane scrollCat = new JScrollPane(catTable);
        scrollCat.setBorder(BorderFactory.createEmptyBorder());
        scrollCat.getViewport().setBackground(Color.WHITE);
        leftPanel.add(scrollCat, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // Right Panel: Audit History Table
        JPanel rightPanel = new JPanel(new BorderLayout(0, 10));
        rightPanel.setBackground(UITheme.CARD_BG);
        rightPanel.setBorder(UITheme.createCardBorder());

        JLabel lblAuditTitle = new JLabel("Lending Transaction Audit Trail");
        lblAuditTitle.setFont(UITheme.FONT_HEADER);
        rightPanel.add(lblAuditTitle, BorderLayout.NORTH);

        String[] auditCols = {"Loan ID", "Book Title", "ISBN", "Member Name", "Issue Date", "Due Date", "Return Date", "Status"};
        auditTableModel = new DefaultTableModel(auditCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        auditTable = new JTable(auditTableModel);
        TableStyler.apply(auditTable);
        auditTable.getColumnModel().getColumn(7).setCellRenderer(TableStyler.createStatusRenderer());

        JScrollPane scrollAudit = new JScrollPane(auditTable);
        scrollAudit.setBorder(BorderFactory.createEmptyBorder());
        scrollAudit.getViewport().setBackground(Color.WHITE);
        rightPanel.add(scrollAudit, BorderLayout.CENTER);

        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    public void loadReports() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            Map<String, Integer> catMap;
            List<LoanTransaction> transactions;

            @Override
            protected Void doInBackground() throws Exception {
                catMap = reportService.getBooksPerCategory();
                transactions = lendingService.getAllLoans(1, 100);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    categoryTableModel.setRowCount(0);
                    if (catMap != null) {
                        for (Map.Entry<String, Integer> e : catMap.entrySet()) {
                            categoryTableModel.addRow(new Object[]{e.getKey(), e.getValue()});
                        }
                    }

                    auditTableModel.setRowCount(0);
                    if (transactions != null) {
                        for (LoanTransaction lt : transactions) {
                            auditTableModel.addRow(new Object[]{
                                    lt.getLoanId(),
                                    lt.getBookTitle(),
                                    lt.getBookIsbn(),
                                    lt.getMemberName(),
                                    DateUtil.format(lt.getIssueDate()),
                                    DateUtil.format(lt.getDueDate()),
                                    lt.getReturnDate() != null ? DateUtil.format(lt.getReturnDate()) : "-",
                                    lt.getStatus().name()
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed loading reports: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void exportAuditToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("lending_audit_report.csv"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileToSave))) {
                // Header
                for (int i = 0; i < auditTableModel.getColumnCount(); i++) {
                    writer.print("\"" + auditTableModel.getColumnName(i) + "\"" + (i == auditTableModel.getColumnCount() - 1 ? "" : ","));
                }
                writer.println();

                // Rows
                for (int r = 0; r < auditTableModel.getRowCount(); r++) {
                    for (int c = 0; c < auditTableModel.getColumnCount(); c++) {
                        Object val = auditTableModel.getValueAt(r, c);
                        writer.print("\"" + (val != null ? val.toString().replace("\"", "\"\"") : "") + "\"" +
                                (c == auditTableModel.getColumnCount() - 1 ? "" : ","));
                    }
                    writer.println();
                }

                JOptionPane.showMessageDialog(this, "Audit report exported successfully to:\n" + fileToSave.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to export CSV: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
