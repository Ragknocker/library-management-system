package com.library.ui;

import com.library.exception.DatabaseException;
import com.library.exception.MemberNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Member;
import com.library.model.MemberType;
import com.library.service.MemberService;
import com.library.ui.components.TableStyler;
import com.library.util.InputValidator;
import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Member Management Panel for patron accounts, membership levels,
 * and lending privileges.
 */
public class MemberManagementPanel extends JPanel {
    private final MemberService memberService;
    private final MainFrame mainFrame;

    private JTextField txtSearch;
    private JComboBox<String> cmbType;
    private DefaultTableModel tableModel;
    private JTable memberTable;

    public MemberManagementPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.memberService = new MemberService();

        setLayout(new BorderLayout(0, 16));
        setBackground(UITheme.BACKGROUND);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        initUI();
    }

    private void initUI() {
        // Header & Toolbar
        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setOpaque(false);

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);

        JLabel lblTitle = new JLabel("Patron Accounts & Member Management");
        lblTitle.setFont(UITheme.FONT_TITLE);
        lblTitle.setForeground(UITheme.TEXT_MAIN);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionBtns.setOpaque(false);

        JButton btnAdd = UITheme.createPrimaryButton("+ Register Member");
        btnAdd.addActionListener(e -> showAddMemberDialog());

        JButton btnEdit = UITheme.createSecondaryButton("✎ Edit Account");
        btnEdit.addActionListener(e -> showEditMemberDialog());

        JButton btnToggleStatus = UITheme.createSecondaryButton("⇄ Toggle Status");
        btnToggleStatus.addActionListener(e -> handleToggleStatus());

        JButton btnDelete = UITheme.createDangerButton("✕ Delete");
        btnDelete.addActionListener(e -> handleDeleteMember());

        actionBtns.add(btnAdd);
        actionBtns.add(btnEdit);
        actionBtns.add(btnToggleStatus);
        actionBtns.add(btnDelete);

        headerBar.add(lblTitle, BorderLayout.WEST);
        headerBar.add(actionBtns, BorderLayout.EAST);
        topContainer.add(headerBar);
        topContainer.add(Box.createVerticalStrut(14));

        // Filter Bar
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterBar.setBackground(UITheme.CARD_BG);
        filterBar.setBorder(UITheme.createCardBorder());

        JLabel lblSearch = new JLabel("Search Patrons:");
        lblSearch.setFont(UITheme.FONT_BOLD);
        txtSearch = UITheme.createTextField();
        txtSearch.setPreferredSize(new Dimension(240, 32));
        txtSearch.setToolTipText("Search by Name, Email, or Member ID");
        txtSearch.addActionListener(e -> loadMembers());

        JLabel lblType = new JLabel("Member Type:");
        lblType.setFont(UITheme.FONT_BOLD);
        cmbType = new JComboBox<>(new String[]{"ALL", "STUDENT", "FACULTY", "COMMUNITY"});
        cmbType.setFont(UITheme.FONT_BODY);
        cmbType.setPreferredSize(new Dimension(160, 32));
        cmbType.addActionListener(e -> loadMembers());

        JButton btnFilter = UITheme.createPrimaryButton("Search");
        btnFilter.addActionListener(e -> loadMembers());

        JButton btnClear = UITheme.createSecondaryButton("Reset");
        btnClear.addActionListener(e -> {
            txtSearch.setText("");
            cmbType.setSelectedIndex(0);
            loadMembers();
        });

        filterBar.add(lblSearch);
        filterBar.add(txtSearch);
        filterBar.add(lblType);
        filterBar.add(cmbType);
        filterBar.add(btnFilter);
        filterBar.add(btnClear);

        topContainer.add(filterBar);
        add(topContainer, BorderLayout.NORTH);

        // Center Table
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setBackground(UITheme.CARD_BG);
        tableContainer.setBorder(UITheme.createCardBorder());

        String[] cols = {"Member ID", "Full Name", "Email Address", "Phone", "Tier", "Max Limit", "Active Loans", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        memberTable = new JTable(tableModel);
        TableStyler.apply(memberTable);
        memberTable.getColumnModel().getColumn(0).setMaxWidth(130);
        memberTable.getColumnModel().getColumn(5).setMaxWidth(90);
        memberTable.getColumnModel().getColumn(6).setMaxWidth(100);
        memberTable.getColumnModel().getColumn(7).setMaxWidth(110);
        memberTable.getColumnModel().getColumn(7).setCellRenderer(TableStyler.createStatusRenderer());

        JScrollPane scrollPane = new JScrollPane(memberTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(tableContainer, BorderLayout.CENTER);
    }

    public void loadMembers() {
        String keyword = txtSearch.getText().trim();
        String type = (String) cmbType.getSelectedItem();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Member> list;

            @Override
            protected Void doInBackground() throws Exception {
                list = memberService.searchMembers(keyword, type, 1, 100);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    tableModel.setRowCount(0);
                    if (list != null) {
                        for (Member m : list) {
                            tableModel.addRow(new Object[]{
                                    m.getMemberId(),
                                    m.getName(),
                                    m.getEmail(),
                                    m.getPhone(),
                                    m.getMemberType().getDisplayName(),
                                    m.getMaxBorrowLimit() + " books",
                                    m.getActiveLoansCount() + " active",
                                    m.getStatus()
                            });
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to load members: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void showAddMemberDialog() {
        JDialog dialog = new JDialog(mainFrame, "Register New Library Member", true);
        dialog.setSize(500, 420);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 12));
        form.setBorder(new EmptyBorder(20, 20, 20, 20));

        JTextField tfMemberId = UITheme.createTextField();
        tfMemberId.setText("LIB-" + (1000 + (int)(Math.random() * 9000)));
        JTextField tfName = UITheme.createTextField();
        JTextField tfEmail = UITheme.createTextField();
        JTextField tfPhone = UITheme.createTextField();

        JComboBox<MemberType> cmbMemberType = new JComboBox<>(MemberType.values());
        cmbMemberType.setFont(UITheme.FONT_BODY);

        JTextField tfMaxLimit = UITheme.createTextField();
        tfMaxLimit.setText(String.valueOf(MemberType.STUDENT.getDefaultMaxLimit()));

        cmbMemberType.addActionListener(e -> {
            MemberType selected = (MemberType) cmbMemberType.getSelectedItem();
            if (selected != null) {
                tfMaxLimit.setText(String.valueOf(selected.getDefaultMaxLimit()));
            }
        });

        form.add(new JLabel("Member ID:*")); form.add(tfMemberId);
        form.add(new JLabel("Full Name:*")); form.add(tfName);
        form.add(new JLabel("Email Address:*")); form.add(tfEmail);
        form.add(new JLabel("Phone Number:*")); form.add(tfPhone);
        form.add(new JLabel("Membership Tier:*")); form.add(cmbMemberType);
        form.add(new JLabel("Max Borrow Allowance:")); form.add(tfMaxLimit);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnCancel = UITheme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnSave = UITheme.createPrimaryButton("Register Account");
        btnSave.addActionListener(e -> {
            try {
                Member m = new Member();
                m.setMemberId(tfMemberId.getText().trim());
                m.setName(tfName.getText().trim());
                m.setEmail(tfEmail.getText().trim());
                m.setPhone(tfPhone.getText().trim());
                MemberType mt = (MemberType) cmbMemberType.getSelectedItem();
                m.setMemberType(mt);
                m.setMaxBorrowLimit(InputValidator.validatePositiveInt(tfMaxLimit.getText(), "Max Borrow Limit"));
                m.setBorrowDurationDays(mt.getDefaultLoanDays());
                m.setStatus("ACTIVE");

                memberService.registerMember(m);
                JOptionPane.showMessageDialog(dialog, "Member " + m.getName() + " registered successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadMembers();
                mainFrame.getDashboardPanel().refreshData();
            } catch (ValidationException | DatabaseException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnBar.add(btnCancel);
        btnBar.add(btnSave);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void showEditMemberDialog() {
        int row = memberTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a member to edit.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String memberId = (String) tableModel.getValueAt(row, 0);
        try {
            Member m = memberService.getMemberById(memberId);

            JDialog dialog = new JDialog(mainFrame, "Edit Member: " + m.getName(), true);
            dialog.setSize(500, 420);
            dialog.setLocationRelativeTo(mainFrame);
            dialog.setLayout(new BorderLayout());

            JPanel form = new JPanel(new GridLayout(6, 2, 10, 12));
            form.setBorder(new EmptyBorder(20, 20, 20, 20));

            JTextField tfMemberId = UITheme.createTextField();
            tfMemberId.setText(m.getMemberId());
            tfMemberId.setEditable(false);
            JTextField tfName = UITheme.createTextField();
            tfName.setText(m.getName());
            JTextField tfEmail = UITheme.createTextField();
            tfEmail.setText(m.getEmail());
            JTextField tfPhone = UITheme.createTextField();
            tfPhone.setText(m.getPhone());

            JComboBox<MemberType> cmbMemberType = new JComboBox<>(MemberType.values());
            cmbMemberType.setSelectedItem(m.getMemberType());

            JTextField tfMaxLimit = UITheme.createTextField();
            tfMaxLimit.setText(String.valueOf(m.getMaxBorrowLimit()));

            form.add(new JLabel("Member ID (Immutable):")); form.add(tfMemberId);
            form.add(new JLabel("Full Name:*")); form.add(tfName);
            form.add(new JLabel("Email Address:*")); form.add(tfEmail);
            form.add(new JLabel("Phone Number:*")); form.add(tfPhone);
            form.add(new JLabel("Membership Tier:*")); form.add(cmbMemberType);
            form.add(new JLabel("Max Borrow Allowance:")); form.add(tfMaxLimit);

            dialog.add(form, BorderLayout.CENTER);

            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
            JButton btnCancel = UITheme.createSecondaryButton("Cancel");
            btnCancel.addActionListener(e -> dialog.dispose());

            JButton btnUpdate = UITheme.createPrimaryButton("Update Account");
            btnUpdate.addActionListener(e -> {
                try {
                    m.setName(tfName.getText().trim());
                    m.setEmail(tfEmail.getText().trim());
                    m.setPhone(tfPhone.getText().trim());
                    MemberType mt = (MemberType) cmbMemberType.getSelectedItem();
                    m.setMemberType(mt);
                    m.setMaxBorrowLimit(InputValidator.validatePositiveInt(tfMaxLimit.getText(), "Max Borrow Limit"));
                    m.setBorrowDurationDays(mt.getDefaultLoanDays());

                    memberService.updateMember(m);
                    JOptionPane.showMessageDialog(dialog, "Member account updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadMembers();
                } catch (Exception ex) {
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

    private void handleToggleStatus() {
        int row = memberTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a member.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String memberId = (String) tableModel.getValueAt(row, 0);
        try {
            Member m = memberService.getMemberById(memberId);
            String newStatus = "ACTIVE".equalsIgnoreCase(m.getStatus()) ? "SUSPENDED" : "ACTIVE";
            m.setStatus(newStatus);
            memberService.updateMember(m);
            loadMembers();
            JOptionPane.showMessageDialog(this, "Member status changed to " + newStatus, "Status Updated", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteMember() {
        int row = memberTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a member to delete.", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String memberId = (String) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete member " + memberId + " (" + name + ")?",
                "Confirm Account Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                memberService.deleteMember(memberId);
                JOptionPane.showMessageDialog(this, "Member account successfully deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
                loadMembers();
                mainFrame.getDashboardPanel().refreshData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Deletion Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
