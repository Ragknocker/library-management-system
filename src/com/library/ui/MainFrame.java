package com.library.ui;

import com.library.config.AppConfig;
import com.library.config.DBConnection;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Main Application Window.
 * Houses the top navigation, connection indicator, and multi-tier Swing tabs.
 */
public class MainFrame extends JFrame {
    private JTabbedPane tabbedPane;
    private JLabel lblDbStatus;
    private JButton btnDbConfig;

    private DashboardPanel dashboardPanel;
    private BookCatalogPanel bookCatalogPanel;
    private LendingDeskPanel lendingDeskPanel;
    private MemberManagementPanel memberManagementPanel;
    private OverdueFinesPanel overdueFinesPanel;
    private ReportsPanel reportsPanel;

    public MainFrame() {
        setTitle("Athena Library Management System • Core Java Multi-Tier Enterprise Suite");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1240, 840);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);

        initUI();
        checkInitialDbConnection();
    }

    private void initUI() {
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UITheme.BACKGROUND);

        // Header Bar
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(UITheme.SECONDARY);
        headerBar.setBorder(new EmptyBorder(14, 24, 14, 24));

        // Brand & Title
        JPanel brandBox = new JPanel();
        brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.Y_AXIS));
        brandBox.setOpaque(false);

        JLabel lblLogo = new JLabel("Athena Library Management System");
        lblLogo.setFont(UITheme.FONT_TITLE);
        lblLogo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Multi-Tier Desktop Architecture | Core Java • MySQL • JDBC • OOP");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(new Color(203, 213, 225));

        brandBox.add(lblLogo);
        brandBox.add(Box.createVerticalStrut(2));
        brandBox.add(lblSub);
        headerBar.add(brandBox, BorderLayout.WEST);

        // Right status and settings
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 4));
        rightHeader.setOpaque(false);

        lblDbStatus = new JLabel("● Checking Database...");
        lblDbStatus.setFont(UITheme.FONT_BOLD);
        lblDbStatus.setForeground(Color.WHITE);

        btnDbConfig = UITheme.createSecondaryButton("⚙ DB Settings");
        btnDbConfig.addActionListener(e -> showSettingsDialog());

        rightHeader.add(lblDbStatus);
        rightHeader.add(btnDbConfig);
        headerBar.add(rightHeader, BorderLayout.EAST);

        rootPanel.add(headerBar, BorderLayout.NORTH);

        // Main Tabbed Navigation
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BOLD);
        tabbedPane.setBackground(UITheme.BACKGROUND);

        dashboardPanel = new DashboardPanel(this);
        bookCatalogPanel = new BookCatalogPanel(this);
        lendingDeskPanel = new LendingDeskPanel(this);
        memberManagementPanel = new MemberManagementPanel(this);
        overdueFinesPanel = new OverdueFinesPanel(this);
        reportsPanel = new ReportsPanel(this);

        tabbedPane.addTab("  Dashboard  ", dashboardPanel);
        tabbedPane.addTab("  Book Catalog  ", bookCatalogPanel);
        tabbedPane.addTab("  Lending Desk  ", lendingDeskPanel);
        tabbedPane.addTab("  Patron Accounts  ", memberManagementPanel);
        tabbedPane.addTab("  Overdue & Fines  ", overdueFinesPanel);
        tabbedPane.addTab("  Reports & Audit  ", reportsPanel);

        tabbedPane.addChangeListener(e -> {
            int selected = tabbedPane.getSelectedIndex();
            switch (selected) {
                case 0 -> dashboardPanel.refreshData();
                case 1 -> bookCatalogPanel.loadBooks();
                case 2 -> {} // Lending desk
                case 3 -> memberManagementPanel.loadMembers();
                case 4 -> overdueFinesPanel.loadData();
                case 5 -> reportsPanel.loadReports();
            }
        });

        rootPanel.add(tabbedPane, BorderLayout.CENTER);
        setContentPane(rootPanel);
    }

    public void switchToTab(int index) {
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(index);
        }
    }

    public void updateDbStatus(boolean connected) {
        String dbType = AppConfig.get("db.type", "mysql").toUpperCase();
        if (connected) {
            String label = "SQLITE".equals(dbType) ? "Embedded Local DB (Active)" : "MySQL 8.0 (" + AppConfig.get("db.name", "library_db") + ")";
            lblDbStatus.setText("● Connected: " + label);
            lblDbStatus.setForeground(new Color(52, 211, 153)); // Emerald green
        } else {
            lblDbStatus.setText("● Disconnected (" + dbType + ") - Click DB Settings");
            lblDbStatus.setForeground(new Color(248, 113, 113)); // Soft red
        }
    }

    public void checkInitialDbConnection() {
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return DBConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    updateDbStatus(ok);
                    if (ok) {
                        refreshAllPanels();
                    } else {
                        // Prompt user to configure connection
                        SwingUtilities.invokeLater(() -> {
                            int opt = JOptionPane.showConfirmDialog(MainFrame.this,
                                    "Could not connect to MySQL with the default credentials.\n" +
                                    "Would you like to open the Database Settings dialog to set your MySQL password\n" +
                                    "and initialize/seed the 1,000+ records?",
                                    "MySQL Connection Setup",
                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (opt == JOptionPane.YES_OPTION) {
                                showSettingsDialog();
                            }
                        });
                    }
                } catch (Exception ignored) {
                    updateDbStatus(false);
                }
            }
        };
        worker.execute();
    }

    public void refreshAllPanels() {
        bookCatalogPanel.loadCategories();
        bookCatalogPanel.loadBooks();
        dashboardPanel.refreshData();
        memberManagementPanel.loadMembers();
    }

    public void showSettingsDialog() {
        new SettingsDialog(this).setVisible(true);
    }

    public DashboardPanel getDashboardPanel() {
        return dashboardPanel;
    }

    public BookCatalogPanel getBookCatalogPanel() {
        return bookCatalogPanel;
    }

    public LendingDeskPanel getLendingDeskPanel() {
        return lendingDeskPanel;
    }

    public MemberManagementPanel getMemberManagementPanel() {
        return memberManagementPanel;
    }
}
