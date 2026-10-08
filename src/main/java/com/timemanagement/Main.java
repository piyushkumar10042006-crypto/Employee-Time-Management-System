package com.timemanagement;

import com.timemanagement.db.DBInitializer;
import com.timemanagement.exception.DatabaseOperationException;
import com.timemanagement.ui.LoginFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main application bootstrap class.
 * Sets modern look-and-feel, verifies database connectivity, and opens the Login UI.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Setup Modern UI Look and Feel (FlatLaf)
        try {
            // Attempt FlatLaf Light Theme
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatLightLaf");
            flatLafClass.getMethod("setup").invoke(null);
        } catch (Exception e) {
            // Fallback to Native System Look and Feel if FlatLaf is not present
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        // 2. Initialize Database & Tables (Schema & Initial Seed Data)
        try {
            DBInitializer.initializeDatabase();
            System.out.println("✓ Database connection established and tables verified.");
        } catch (DatabaseOperationException ex) {
            System.err.println("⚠ Database initialization warning: " + ex.getMessage());
            // Show helpful dialog with troubleshooting tips
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Could not connect to MySQL database.\n\n" +
                        "Details: " + ex.getMessage() + "\n\n" +
                        "Please verify that MySQL is running on localhost:3306 and check db.properties.",
                        "Database Connection Notice", JOptionPane.WARNING_MESSAGE);
            });
        }

        // 3. Launch Graphical User Interface on Swing Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
