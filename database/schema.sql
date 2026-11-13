CREATE DATABASE IF NOT EXISTS smart_parking_db;
USE smart_parking_db;

CREATE TABLE IF NOT EXISTS parking_slots (
    slot_id INT PRIMARY KEY AUTO_INCREMENT,
    slot_number VARCHAR(10) NOT NULL UNIQUE,
    is_occupied BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS parking_records (
    record_id INT PRIMARY KEY AUTO_INCREMENT,
    vehicle_number VARCHAR(20) NOT NULL,
    vehicle_type VARCHAR(30) NOT NULL,
    slot_id INT NOT NULL,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME NULL,
    fee DECIMAL(10,2) NULL,
    status ENUM('PARKED', 'COMPLETED') NOT NULL DEFAULT 'PARKED',
    CONSTRAINT fk_record_slot FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id)
);

INSERT IGNORE INTO parking_slots (slot_number, is_occupied) VALUES
('A-01', FALSE), ('A-02', FALSE), ('A-03', FALSE), ('A-04', FALSE), ('A-05', FALSE),
('B-01', FALSE), ('B-02', FALSE), ('B-03', FALSE), ('B-04', FALSE), ('B-05', FALSE);
