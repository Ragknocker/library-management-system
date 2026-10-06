package com.library;

import com.library.ui.MainFrame;

import javax.swing.*;
import java.awt.*;

/**
 * Application Entry Point.
 * Configures Modern Look and Feel, UI Antialiasing,
 * and initializes the main desktop application frame.
 */
public class Main {
    public static void main(String[] args) {
        // Configure Look and Feel
        try {
            // Attempt FlatLaf modern UI theme
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            flatLafClass.getMethod("setup").invoke(null);
        } catch (Throwable t) {
            // Graceful fallback to System Look and Feel
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // Enable high-DPI text antialiasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Launch UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to launch application: " + e.getMessage(),
                        "Fatal Launch Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
