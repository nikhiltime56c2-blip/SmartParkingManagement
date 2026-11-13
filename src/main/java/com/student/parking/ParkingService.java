package com.student.parking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ParkingService {
    private static final BigDecimal RATE_PER_HOUR = new BigDecimal("20.00");

    public static class ActiveVehicle {
        public final int recordId;
        public final String vehicleNumber;
        public final String vehicleType;
        public final String slotNumber;
        public final LocalDateTime entryTime;

        public ActiveVehicle(int recordId, String vehicleNumber, String vehicleType,
                             String slotNumber, LocalDateTime entryTime) {
            this.recordId = recordId;
            this.vehicleNumber = vehicleNumber;
            this.vehicleType = vehicleType;
            this.slotNumber = slotNumber;
            this.entryTime = entryTime;
        }
    }

    public static class HistoryRecord {
        public final String vehicleNumber;
        public final String vehicleType;
        public final String slotNumber;
        public final LocalDateTime entryTime;
        public final LocalDateTime exitTime;
        public final BigDecimal fee;

        public HistoryRecord(String vehicleNumber, String vehicleType, String slotNumber,
                             LocalDateTime entryTime, LocalDateTime exitTime, BigDecimal fee) {
            this.vehicleNumber = vehicleNumber;
            this.vehicleType = vehicleType;
            this.slotNumber = slotNumber;
            this.entryTime = entryTime;
            this.exitTime = exitTime;
            this.fee = fee;
        }
    }

    public int[] getSlotCounts() throws Exception {
        String sql = "SELECT COUNT(*) AS total, " +
                "SUM(CASE WHEN is_occupied = 1 THEN 1 ELSE 0 END) AS occupied " +
                "FROM parking_slots";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            int total = rs.getInt("total");
            int occupied = rs.getInt("occupied");
            return new int[]{total, occupied, total - occupied};
        }
    }

    public String parkVehicle(String vehicleNumber, String vehicleType) throws Exception {
        String number = vehicleNumber.trim().toUpperCase();
        if (number.isEmpty()) {
            throw new IllegalArgumentException("Please enter a vehicle number.");
        }
        if (vehicleType == null || vehicleType.isBlank()) {
            throw new IllegalArgumentException("Please select a vehicle type.");
        }

        String findDuplicate = "SELECT record_id FROM parking_records " +
                "WHERE vehicle_number = ? AND status = 'PARKED'";
        String findSlot = "SELECT slot_id, slot_number FROM parking_slots " +
                "WHERE is_occupied = 0 ORDER BY slot_id LIMIT 1 FOR UPDATE";

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(findDuplicate)) {
                    ps.setString(1, number);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            throw new IllegalArgumentException("This vehicle is already parked.");
                        }
                    }
                }

                int slotId;
                String slotNumber;
                try (PreparedStatement ps = con.prepareStatement(findSlot);
                     ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new IllegalStateException("Parking is full. No slot is available.");
                    }
                    slotId = rs.getInt("slot_id");
                    slotNumber = rs.getString("slot_number");
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE parking_slots SET is_occupied = 1 WHERE slot_id = ?")) {
                    ps.setInt(1, slotId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO parking_records " +
                                "(vehicle_number, vehicle_type, slot_id, entry_time, status) " +
                                "VALUES (?, ?, ?, NOW(), 'PARKED')")) {
                    ps.setString(1, number);
                    ps.setString(2, vehicleType);
                    ps.setInt(3, slotId);
                    ps.executeUpdate();
                }

                con.commit();
                return slotNumber;
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<ActiveVehicle> getActiveVehicles() throws Exception {
        List<ActiveVehicle> vehicles = new ArrayList<>();
        String sql = "SELECT r.record_id, r.vehicle_number, r.vehicle_type, " +
                "s.slot_number, r.entry_time FROM parking_records r " +
                "JOIN parking_slots s ON r.slot_id = s.slot_id " +
                "WHERE r.status = 'PARKED' ORDER BY r.entry_time DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                vehicles.add(new ActiveVehicle(
                        rs.getInt("record_id"),
                        rs.getString("vehicle_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("slot_number"),
                        rs.getTimestamp("entry_time").toLocalDateTime()
                ));
            }
        }
        return vehicles;
    }

    public BigDecimal exitVehicle(int recordId) throws Exception {
        String find = "SELECT slot_id, entry_time FROM parking_records " +
                "WHERE record_id = ? AND status = 'PARKED' FOR UPDATE";
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int slotId;
                LocalDateTime entry;
                try (PreparedStatement ps = con.prepareStatement(find)) {
                    ps.setInt(1, recordId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("The selected parking record is no longer active.");
                        }
                        slotId = rs.getInt("slot_id");
                        entry = rs.getTimestamp("entry_time").toLocalDateTime();
                    }
                }

                LocalDateTime exit = LocalDateTime.now();
                long minutes = Math.max(1, Duration.between(entry, exit).toMinutes());
                long billableHours = Math.max(1, (minutes + 59) / 60);
                BigDecimal fee = RATE_PER_HOUR.multiply(BigDecimal.valueOf(billableHours))
                        .setScale(2, RoundingMode.HALF_UP);

                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE parking_records SET exit_time = NOW(), fee = ?, status = 'COMPLETED' " +
                                "WHERE record_id = ?")) {
                    ps.setBigDecimal(1, fee);
                    ps.setInt(2, recordId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE parking_slots SET is_occupied = 0 WHERE slot_id = ?")) {
                    ps.setInt(1, slotId);
                    ps.executeUpdate();
                }

                con.commit();
                return fee;
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public List<HistoryRecord> getHistory() throws Exception {
        List<HistoryRecord> records = new ArrayList<>();
        String sql = "SELECT r.vehicle_number, r.vehicle_type, s.slot_number, " +
                "r.entry_time, r.exit_time, r.fee FROM parking_records r " +
                "JOIN parking_slots s ON r.slot_id = s.slot_id " +
                "WHERE r.status = 'COMPLETED' ORDER BY r.exit_time DESC LIMIT 100";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                records.add(new HistoryRecord(
                        rs.getString("vehicle_number"),
                        rs.getString("vehicle_type"),
                        rs.getString("slot_number"),
                        rs.getTimestamp("entry_time").toLocalDateTime(),
                        rs.getTimestamp("exit_time").toLocalDateTime(),
                        rs.getBigDecimal("fee")
                ));
            }
        }
        return records;
    }
}
