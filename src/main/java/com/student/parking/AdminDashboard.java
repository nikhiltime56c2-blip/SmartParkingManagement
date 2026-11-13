
package com.student.parking;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminDashboard extends JFrame {

    private JLabel revenueLabel;
    private JLabel todayLabel;
    private JLabel occupiedLabel;
    private JLabel availableLabel;

    private JTextField searchField;
    private JTable historyTable;
    private DefaultTableModel tableModel;
    private JPanel parkingMapPanel;

    public AdminDashboard() {

        setTitle("Smart Parking - Admin Dashboard");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout(15, 15));

        // Header
        JLabel heading = new JLabel("ADMIN DASHBOARD");
        heading.setFont(new Font("Arial", Font.BOLD, 27));
        heading.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        add(heading, BorderLayout.NORTH);

        // Statistics cards
        JPanel cards = new JPanel(new GridLayout(1, 4, 15, 15));
        cards.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        revenueLabel = new JLabel("₹0.00");
        todayLabel = new JLabel("0");
        occupiedLabel = new JLabel("0");
        availableLabel = new JLabel("0");

        cards.add(createCard("Total Revenue", revenueLabel));
        cards.add(createCard("Vehicles Today", todayLabel));
        cards.add(createCard("Occupied Slots", occupiedLabel));
        cards.add(createCard("Available Slots", availableLabel));

        // Search section
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchField = new JTextField(22);

        JButton searchButton = new JButton("Search");
        JButton refreshButton = new JButton("Refresh");

        searchPanel.add(new JLabel("Search Vehicle:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(refreshButton);

        // Live parking map
        parkingMapPanel = new JPanel(
            new GridLayout(0, 5, 10, 10)
        );

        parkingMapPanel.setBorder(
            BorderFactory.createTitledBorder("Live Parking Map")
        );

        parkingMapPanel.setBackground(Color.WHITE);
        
        // History table
        String[] columns = {
            "Vehicle Number", "Vehicle Type", "Slot",
            "Entry Time", "Exit Time", "Fee (INR)", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(tableModel);
        historyTable.setRowHeight(28);

        JScrollPane scrollPane = new JScrollPane(historyTable);


        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(
            new BoxLayout(centerPanel, BoxLayout.Y_AXIS)
        );

        centerPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 20, 20, 20)
        );

        cards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        parkingMapPanel.setPreferredSize(new Dimension(900, 160));
        parkingMapPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        scrollPane.setPreferredSize(new Dimension(900, 250));

        centerPanel.add(cards);
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.add(parkingMapPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(searchPanel);
        centerPanel.add(scrollPane);

        add(centerPanel, BorderLayout.CENTER);

        // Button actions
        searchButton.addActionListener(e ->
            loadHistory(searchField.getText().trim())
        );

        refreshButton.addActionListener(e -> refreshDashboard());

        refreshDashboard();
    }

    private JPanel createCard(String title, JLabel value) {

        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));

        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        panel.setBackground(new Color(235, 242, 250));

        JLabel heading = new JLabel(title);
        heading.setFont(new Font("Arial", Font.BOLD, 15));

        value.setFont(new Font("Arial", Font.BOLD, 24));
        value.setForeground(new Color(20, 80, 140));

        panel.add(heading);
        panel.add(value);

        return panel;
    }

    private void refreshDashboard() {

        try (Connection con = DBConnection.getConnection()) {

            // Total revenue
            String revenueSQL =
                "SELECT COALESCE(SUM(fee), 0) " +
                "FROM parking_records WHERE status = 'COMPLETED'";

            try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(revenueSQL)) {

                if (rs.next()) {
                    revenueLabel.setText(
                        "₹" + String.format("%.2f", rs.getDouble(1))
                    );
                }
            }

            // Vehicles parked today
            String todaySQL =
                "SELECT COUNT(*) FROM parking_records " +
                "WHERE DATE(entry_time) = CURDATE()";

            try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(todaySQL)) {

                if (rs.next()) {
                    todayLabel.setText(String.valueOf(rs.getInt(1)));
                }
            }

            // Occupied and available slots
            String slotSQL =
                "SELECT COUNT(*), " +
                "COALESCE(SUM(is_occupied), 0) " +
                "FROM parking_slots";

            try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery(slotSQL)) {

                if (rs.next()) {

                    int total = rs.getInt(1);
                    int occupied = rs.getInt(2);

                    occupiedLabel.setText(String.valueOf(occupied));
                    availableLabel.setText(
                        String.valueOf(total - occupied)
                    );
                }
            }

            loadHistory("");
            loadParkingSlots();

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Dashboard error: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void loadHistory(String vehicleNumber) {

        tableModel.setRowCount(0);

        String sql =
            "SELECT r.vehicle_number, r.vehicle_type, s.slot_number, " +
            "r.entry_time, r.exit_time, r.fee, r.status " +
            "FROM parking_records r " +
            "JOIN parking_slots s ON r.slot_id = s.slot_id " +
            "WHERE r.vehicle_number LIKE ? " +
            "ORDER BY r.entry_time DESC LIMIT 100";

        try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + vehicleNumber + "%");

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    tableModel.addRow(new Object[] {
                        rs.getString("vehicle_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("slot_number"),
                        rs.getTimestamp("entry_time"),
                        rs.getTimestamp("exit_time"),
                        rs.getObject("fee"),
                        rs.getString("status")
                    });
                }
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "History error: " + ex.getMessage()
            );
        }
    }

    
    private void loadParkingSlots() {

        parkingMapPanel.removeAll();

        String sql =
            "SELECT slot_number, is_occupied " +
            "FROM parking_slots ORDER BY slot_number";

        try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {

                String slotNumber = rs.getString("slot_number");
                boolean occupied = rs.getBoolean("is_occupied");

                JButton slotButton = new JButton(
                    "<html><center>" + slotNumber +
                    "<br><b>" +
                    (occupied ? "OCCUPIED" : "AVAILABLE") +
                    "</b></center></html>"
                );

                slotButton.setFont(new Font("Arial", Font.BOLD, 14));

                slotButton.setForeground(Color.WHITE);

                slotButton.setBackground(
                    occupied
                        ? new Color(210, 65, 65)
                        : new Color(40, 160, 90)
                );

                slotButton.setOpaque(true);
                slotButton.setContentAreaFilled(true);
                slotButton.setBorderPainted(false);
                slotButton.setFocusPainted(false);

                slotButton.setPreferredSize(
                    new Dimension(125, 65)
                );

                // Make parking slots interactive
                slotButton.setCursor(
                    new Cursor(Cursor.HAND_CURSOR)
                );

                slotButton.addActionListener(e ->
                    showSlotDetails(slotNumber)
                );

                parkingMapPanel.add(slotButton);
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not load parking slots: " + ex.getMessage(),
                "Parking Map Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        parkingMapPanel.revalidate();
        parkingMapPanel.repaint();
    }


    private void showSlotDetails(String slotNumber) {

        String sql =
            "SELECT s.is_occupied, r.vehicle_number, " +
            "r.vehicle_type, r.entry_time " +
            "FROM parking_slots s " +
            "LEFT JOIN parking_records r " +
            "ON s.slot_id = r.slot_id " +
            "AND r.status = 'PARKED' " +
            "WHERE s.slot_number = ? " +
            "ORDER BY r.entry_time DESC LIMIT 1";

        try (Connection con = DBConnection.getConnection();
            PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, slotNumber);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    boolean occupied = rs.getBoolean("is_occupied");

                    if (occupied) {

                        String vehicle = rs.getString("vehicle_number");
                        String type = rs.getString("vehicle_type");
                        Timestamp entry = rs.getTimestamp("entry_time");

                        JOptionPane.showMessageDialog(
                            this,
                            "Slot: " + slotNumber +
                            "\nStatus: OCCUPIED" +
                            "\nVehicle Number: " + vehicle +
                            "\nVehicle Type: " + type +
                            "\nEntry Time: " + entry,
                            "Parking Slot Details",
                            JOptionPane.INFORMATION_MESSAGE
                        );

                    } else {

                        JOptionPane.showMessageDialog(
                            this,
                            "Slot: " + slotNumber +
                            "\nStatus: AVAILABLE\n\nThis slot is currently empty.",
                            "Parking Slot Details",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                }
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                this,
                "Could not fetch slot details: " + ex.getMessage(),
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
