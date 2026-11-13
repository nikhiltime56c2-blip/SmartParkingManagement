package com.student.parking;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainFrame extends JFrame {
    private final ParkingService service = new ParkingService();
    private final JTextField vehicleNumberField = new JTextField(14);
    private final JComboBox<String> vehicleTypeBox =
            new JComboBox<>(new String[]{"Two Wheeler", "Four Wheeler", "Other"});
    private final JLabel totalLabel = new JLabel("Total slots: -");
    private final JLabel occupiedLabel = new JLabel("Occupied: -");
    private final JLabel availableLabel = new JLabel("Available: -");
    private final DefaultTableModel activeModel = new DefaultTableModel(
            new String[]{"Record ID", "Vehicle Number", "Type", "Slot", "Entry Time"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[]{"Vehicle Number", "Type", "Slot", "Entry Time", "Exit Time", "Fee (INR)"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable activeTable = new JTable(activeModel);
    private final JTable historyTable = new JTable(historyModel);
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public MainFrame() {
        setTitle("Smart Parking Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 650);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        buildUI();
        refreshAll();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBorder(new EmptyBorder(16, 16, 16, 16));
        main.setBackground(new Color(245, 247, 250));
        setContentPane(main);

        JLabel heading = new JLabel("Smart Parking Management System");
        heading.setFont(new Font("SansSerif", Font.BOLD, 23));
        JLabel subheading = new JLabel("Vehicle entry, exit and parking records");
        subheading.setForeground(new Color(90, 100, 115));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        titlePanel.add(heading);
        titlePanel.add(subheading);

        JPanel stats = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 10));
        stats.setBackground(Color.WHITE);
        stats.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 232)));
        stats.add(totalLabel);
        stats.add(occupiedLabel);
        stats.add(availableLabel);

    JPanel top = new JPanel(new BorderLayout(0, 10));
    top.setOpaque(false);

    // Admin dashboard button
    JButton adminButton = new JButton("Admin Dashboard");
    adminButton.setFont(new Font("SansSerif", Font.BOLD, 13));

    adminButton.addActionListener(e -> {
        new AdminDashboard().setVisible(true);
    });

    // Header with title and dashboard button
    JPanel header = new JPanel(new BorderLayout(10, 0));
    header.setOpaque(false);
    header.add(titlePanel, BorderLayout.CENTER);
    header.add(adminButton, BorderLayout.EAST);

    top.add(header, BorderLayout.NORTH);
    top.add(stats, BorderLayout.SOUTH);

    main.add(top, BorderLayout.NORTH);


        JPanel entryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        entryPanel.setBackground(Color.WHITE);
        entryPanel.setBorder(BorderFactory.createTitledBorder("New vehicle entry"));
        entryPanel.add(new JLabel("Vehicle number:"));
        entryPanel.add(vehicleNumberField);
        entryPanel.add(new JLabel("Vehicle type:"));
        entryPanel.add(vehicleTypeBox);
        JButton parkButton = new JButton("Park Vehicle");
        parkButton.addActionListener(e -> parkVehicle());
        entryPanel.add(parkButton);

        activeTable.setRowHeight(25);
        historyTable.setRowHeight(25);
        activeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeTable.getColumnModel().getColumn(0).setMinWidth(0);
        activeTable.getColumnModel().getColumn(0).setMaxWidth(0);
        activeTable.getColumnModel().getColumn(0).setWidth(0);

        JButton exitButton = new JButton("Vehicle Exit");
        exitButton.addActionListener(e -> exitVehicle());
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshAll());

        JPanel activePanel = new JPanel(new BorderLayout(8, 8));
        activePanel.setBackground(Color.WHITE);
        activePanel.setBorder(BorderFactory.createTitledBorder("Currently parked vehicles"));
        activePanel.add(new JScrollPane(activeTable), BorderLayout.CENTER);
        JPanel activeActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        activeActions.setOpaque(false);
        activeActions.add(refreshButton);
        activeActions.add(exitButton);
        activePanel.add(activeActions, BorderLayout.SOUTH);

        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBackground(Color.WHITE);
        historyPanel.setBorder(BorderFactory.createTitledBorder("Recent parking history (last 100 records)"));
        historyPanel.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(entryPanel, BorderLayout.NORTH);
        center.add(activePanel, BorderLayout.CENTER);
        center.add(historyPanel, BorderLayout.SOUTH);
        historyPanel.setPreferredSize(new Dimension(900, 190));
        main.add(center, BorderLayout.CENTER);

        JLabel footer = new JLabel("Parking rate: ₹20 per started hour");
        footer.setForeground(new Color(90, 100, 115));
        main.add(footer, BorderLayout.SOUTH);
    }

    private void parkVehicle() {
        try {
            String slot = service.parkVehicle(
                    vehicleNumberField.getText(),
                    (String) vehicleTypeBox.getSelectedItem()
            );
            JOptionPane.showMessageDialog(this, "Vehicle parked successfully.\nAssigned slot: " + slot);
            vehicleNumberField.setText("");
            refreshAll();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void exitVehicle() {
        int row = activeTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a parked vehicle first.");
            return;
        }
        int recordId = Integer.parseInt(activeModel.getValueAt(row, 0).toString());
        int confirm = JOptionPane.showConfirmDialog(this,
                "Confirm vehicle exit?", "Vehicle Exit", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        try {
            java.math.BigDecimal fee = service.exitVehicle(recordId);
            JOptionPane.showMessageDialog(this, "Vehicle exit recorded.\nParking fee: ₹" + fee);
            refreshAll();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void refreshAll() {
        try {
            refreshStats();
            refreshActive();
            refreshHistory();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void refreshStats() throws Exception {
        int[] counts = service.getSlotCounts();
        totalLabel.setText("Total slots: " + counts[0]);
        occupiedLabel.setText("Occupied: " + counts[1]);
        availableLabel.setText("Available: " + counts[2]);
    }

    private void refreshActive() throws Exception {
        activeModel.setRowCount(0);
        List<ParkingService.ActiveVehicle> vehicles = service.getActiveVehicles();
        for (ParkingService.ActiveVehicle v : vehicles) {
            activeModel.addRow(new Object[]{
                    v.recordId, v.vehicleNumber, v.vehicleType, v.slotNumber, FORMAT.format(v.entryTime)
            });
        }
    }

    private void refreshHistory() throws Exception {
        historyModel.setRowCount(0);
        List<ParkingService.HistoryRecord> records = service.getHistory();
        for (ParkingService.HistoryRecord r : records) {
            historyModel.addRow(new Object[]{
                    r.vehicleNumber, r.vehicleType, r.slotNumber,
                    FORMAT.format(r.entryTime), FORMAT.format(r.exitTime), r.fee
            });
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
