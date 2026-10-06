package com.library.ui;

import com.library.config.AppConfig;
import com.library.config.DBConnection;
import com.library.seeder.DataSeeder;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;

/**
 * Settings Dialog supporting both MySQL 8.0 Server and Zero-Config Embedded SQLite.
 */
public class SettingsDialog extends JDialog {
    private final MainFrame mainFrame;

    private JRadioButton rbMysql;
    private JRadioButton rbSqlite;

    private JTextField tfHost;
    private JTextField tfPort;
    private JTextField tfDatabase;
    private JTextField tfUser;
    private JPasswordField tfPassword;
    private JLabel lblStatus;

    public SettingsDialog(MainFrame mainFrame) {
        super(mainFrame, "Database Connection Settings & Data Seeder", true);
        this.mainFrame = mainFrame;

        setSize(580, 560);
        setLocationRelativeTo(mainFrame);
        setLayout(new BorderLayout());
        setResizable(false);

        initUI();
    }

    private void initUI() {
        JPanel container = new JPanel(new BorderLayout(0, 14));
        container.setBackground(Color.WHITE);
        container.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header Title Info
        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel lblHeading = new JLabel("Database Configuration");
        lblHeading.setFont(UITheme.FONT_TITLE);
        lblHeading.setForeground(UITheme.TEXT_MAIN);

        JLabel lblDesc = new JLabel("Choose MySQL Server or Zero-Config Embedded Mode to run immediately.");
        lblDesc.setFont(UITheme.FONT_SUBTITLE);
        lblDesc.setForeground(UITheme.TEXT_MUTED);

        titleBox.add(lblHeading);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(lblDesc);
        container.add(titleBox, BorderLayout.NORTH);

        // Center configuration
        JPanel centerBox = new JPanel();
        centerBox.setLayout(new BoxLayout(centerBox, BoxLayout.Y_AXIS));
        centerBox.setOpaque(false);

        // Radio button selector
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        radioPanel.setOpaque(false);
        radioPanel.setBorder(BorderFactory.createTitledBorder("Database Engine"));

        String currentType = AppConfig.get("db.type", "mysql").toLowerCase();
        rbMysql = new JRadioButton("MySQL Server (localhost:3306)", "mysql".equals(currentType));
        rbMysql.setFont(UITheme.FONT_BOLD);
        rbMysql.setOpaque(false);

        rbSqlite = new JRadioButton("Embedded Local Mode (Zero-Config / No Password)", "sqlite".equals(currentType));
        rbSqlite.setFont(UITheme.FONT_BOLD);
        rbSqlite.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        group.add(rbMysql);
        group.add(rbSqlite);
        radioPanel.add(rbMysql);
        radioPanel.add(rbSqlite);

        centerBox.add(radioPanel);
        centerBox.add(Box.createVerticalStrut(10));

        // Form Fields
        JPanel form = new JPanel(new GridLayout(5, 2, 10, 8));
        form.setOpaque(false);

        tfHost = UITheme.createTextField();
        tfHost.setText(AppConfig.get("db.host", "localhost"));

        tfPort = UITheme.createTextField();
        tfPort.setText(AppConfig.get("db.port", "3306"));

        tfDatabase = UITheme.createTextField();
        tfDatabase.setText(AppConfig.get("db.name", "library_db"));

        tfUser = UITheme.createTextField();
        tfUser.setText(AppConfig.get("db.user", "root"));

        tfPassword = new JPasswordField(AppConfig.get("db.password", ""));
        tfPassword.setFont(UITheme.FONT_BODY);

        form.add(new JLabel("MySQL Host:")); form.add(tfHost);
        form.add(new JLabel("Port:")); form.add(tfPort);
        form.add(new JLabel("Database Name:")); form.add(tfDatabase);
        form.add(new JLabel("Username (default: root):")); form.add(tfUser);
        form.add(new JLabel("MySQL Password:")); form.add(tfPassword);

        // Toggle form fields based on radio selection
        Runnable updateFieldsState = () -> {
            boolean isMysql = rbMysql.isSelected();
            tfHost.setEnabled(isMysql);
            tfPort.setEnabled(isMysql);
            tfDatabase.setEnabled(isMysql);
            tfUser.setEnabled(isMysql);
            tfPassword.setEnabled(isMysql);
        };
        rbMysql.addActionListener(e -> updateFieldsState.run());
        rbSqlite.addActionListener(e -> updateFieldsState.run());
        updateFieldsState.run();

        centerBox.add(form);
        centerBox.add(Box.createVerticalStrut(12));

        lblStatus = new JLabel("Ready. If MySQL password is not known, select 'Embedded Local Mode'.");
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(UITheme.TEXT_MUTED);
        centerBox.add(lblStatus);

        container.add(centerBox, BorderLayout.CENTER);

        // Action Buttons
        JPanel btnBar = new JPanel(new GridLayout(2, 2, 10, 8));
        btnBar.setOpaque(false);

        JButton btnTest = UITheme.createSecondaryButton("Test Connection");
        btnTest.addActionListener(e -> testConnection());

        JButton btnSave = UITheme.createSecondaryButton("Save Settings");
        btnSave.addActionListener(e -> saveConfiguration());

        JButton btnSeed = UITheme.createPrimaryButton("⚡ Initialize & Seed 1,000 Records");
        btnSeed.setBackground(UITheme.SUCCESS);
        btnSeed.addActionListener(e -> initializeAndSeed());

        JButton btnClose = UITheme.createSecondaryButton("Close");
        btnClose.addActionListener(e -> dispose());

        btnBar.add(btnTest);
        btnBar.add(btnSave);
        btnBar.add(btnSeed);
        btnBar.add(btnClose);

        container.add(btnBar, BorderLayout.SOUTH);
        add(container);
    }

    private void applySettingsToConfig() {
        if (rbSqlite.isSelected()) {
            AppConfig.set("db.type", "sqlite");
            AppConfig.set("db.url", "jdbc:sqlite:library.db");
        } else {
            String host = tfHost.getText().trim();
            String port = tfPort.getText().trim();
            String dbName = tfDatabase.getText().trim();
            String user = tfUser.getText().trim();
            String password = new String(tfPassword.getPassword());

            AppConfig.set("db.type", "mysql");
            AppConfig.set("db.host", host);
            AppConfig.set("db.port", port);
            AppConfig.set("db.name", dbName);
            AppConfig.set("db.user", user);
            AppConfig.set("db.password", password);
            AppConfig.set("db.url", "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                    "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }
    }

    private void testConnection() {
        applySettingsToConfig();
        lblStatus.setText("Testing database connectivity...");
        lblStatus.setForeground(UITheme.PRIMARY);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return DBConnection.testConnection();
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        lblStatus.setText("✓ Connected successfully!");
                        lblStatus.setForeground(UITheme.SUCCESS);
                        mainFrame.updateDbStatus(true);
                    } else {
                        if (rbMysql.isSelected()) {
                            lblStatus.setText("✗ Access Denied! Check MySQL root password or select 'Embedded Local Mode'.");
                        } else {
                            lblStatus.setText("✗ Connection failed.");
                        }
                        lblStatus.setForeground(UITheme.DANGER);
                        mainFrame.updateDbStatus(false);
                    }
                } catch (Exception ex) {
                    lblStatus.setText("✗ Error: " + ex.getMessage());
                    lblStatus.setForeground(UITheme.DANGER);
                }
            }
        };
        worker.execute();
    }

    private void saveConfiguration() {
        applySettingsToConfig();
        try {
            AppConfig.saveConfig();
            JOptionPane.showMessageDialog(this, "Settings saved successfully!", "Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Could not save settings: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeAndSeed() {
        applySettingsToConfig();
        saveConfiguration();

        lblStatus.setText("Initializing tables and generating 1,000+ inventory records...");
        lblStatus.setForeground(UITheme.PRIMARY);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                DBConnection.initializeDatabaseIfMissing();
                try (Connection conn = DBConnection.getInstance().getConnection()) {
                    DataSeeder.seedAll(conn, 1000);
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lblStatus.setText("✓ Database initialized with 1,000+ records!");
                    lblStatus.setForeground(UITheme.SUCCESS);
                    JOptionPane.showMessageDialog(SettingsDialog.this,
                            "Success! Database schema deployed and 1,000+ books seeded.",
                            "Initialization Complete", JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.updateDbStatus(true);
                    mainFrame.refreshAllPanels();
                } catch (Exception ex) {
                    lblStatus.setText("✗ Error: " + ex.getMessage());
                    lblStatus.setForeground(UITheme.DANGER);
                    JOptionPane.showMessageDialog(SettingsDialog.this,
                            "Failed: " + ex.getMessage() + "\n\nTip: If you do not have your MySQL password, choose 'Embedded Local Mode' to run instantly!",
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
