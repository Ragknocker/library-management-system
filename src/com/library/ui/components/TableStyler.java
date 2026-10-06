package com.library.ui.components;

import com.library.util.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Custom table renderers and theme stylers for Swing JTables.
 */
public class TableStyler {

    public static void apply(JTable table) {
        table.setRowHeight(34);
        table.setFont(UITheme.FONT_BODY);
        table.setSelectionBackground(UITheme.PRIMARY_LIGHT);
        table.setSelectionForeground(UITheme.TEXT_MAIN);
        table.setGridColor(UITheme.BORDER);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(UITheme.FONT_BOLD);
        header.setBackground(new Color(241, 245, 249));
        header.setForeground(UITheme.TEXT_MAIN);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setReorderingAllowed(false);

        // Alternating row renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(UITheme.TEXT_MAIN);
                }
                setBorder(noFocusBorder);
                return c;
            }
        });
    }

    /**
     * Renders a badge for status columns (ACTIVE, RETURNED, OVERDUE, etc.).
     */
    public static DefaultTableCellRenderer createStatusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                String val = value != null ? value.toString() : "";

                if (!isSelected) {
                    lbl.setOpaque(true);
                    if (val.contains("ACTIVE") || val.contains("In Stock") || val.contains("PAID")) {
                        lbl.setBackground(UITheme.SUCCESS_LIGHT);
                        lbl.setForeground(new Color(6, 95, 70));
                    } else if (val.contains("OVERDUE") || val.contains("Out of Stock") || val.contains("SUSPENDED")) {
                        lbl.setBackground(UITheme.DANGER_LIGHT);
                        lbl.setForeground(new Color(153, 27, 27));
                    } else if (val.contains("RETURNED") || val.contains("WAIVED")) {
                        lbl.setBackground(new Color(241, 245, 249));
                        lbl.setForeground(UITheme.TEXT_MUTED);
                    } else if (val.contains("PENDING")) {
                        lbl.setBackground(UITheme.WARNING_LIGHT);
                        lbl.setForeground(new Color(146, 64, 14));
                    } else {
                        lbl.setBackground(Color.WHITE);
                        lbl.setForeground(UITheme.TEXT_MAIN);
                    }
                }
                lbl.setFont(UITheme.FONT_BOLD);
                return lbl;
            }
        };
    }
}
