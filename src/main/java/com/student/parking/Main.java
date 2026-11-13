package com.student.parking;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Use Swing's default look and feel if the system theme is unavailable.
        }

        SwingUtilities.invokeLater(() -> {
            try {
                DBConnection.getConnection().close();
                new MainFrame().setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(
                        null,
                        "Could not connect to MySQL.\n\n" + ex.getMessage()
                                + "\n\nCheck that MySQL is running and db.properties is correct.",
                        "Database connection error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }
}
