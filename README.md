# Smart Parking Management System

A simple Java Swing + MySQL desktop project for a second-year B.Tech demonstration.

## Features
- Register an arriving vehicle and assign the next available parking slot.
- Prevent duplicate active parking for the same vehicle number.
- Record vehicle exit and calculate parking charges.
- View currently parked vehicles and past parking records.
- Show available and occupied slot counts.

## Technology
- Java 17
- Java Swing (desktop UI)
- MySQL 8.x
- JDBC (MySQL Connector/J)
- Maven (dependency management and running)

## 1. Install the requirements
1. Install JDK 17 or later.
2. Install MySQL Server and make sure it is running.
3. Install VS Code.
4. In VS Code, install **Extension Pack for Java**. Maven support is included in this pack.

## 2. Create the database
1. Open MySQL Workbench (or the MySQL command line).
2. Open `database/schema.sql`.
3. Execute the complete script. It creates `smart_parking_db` and 10 parking slots.

## 3. Set your MySQL password
Open `src/main/resources/db.properties`.

Set `db.user` and `db.password` to your own MySQL credentials. The default user is `root`; the password is blank. Change it if your MySQL installation uses a password.

## 4. Open and run in VS Code
1. Extract the ZIP file.
2. In VS Code choose **File → Open Folder** and select the extracted `SmartParkingManagement` folder (the folder containing `pom.xml`).
3. Wait for Java/Maven extensions to finish importing the project.
4. Open the terminal in VS Code (**Terminal → New Terminal**).
5. Run:

   ```bash
   mvn clean compile
   mvn exec:java
   ```

If `mvn` is not recognized, install Apache Maven and add its `bin` folder to the Windows PATH, then restart VS Code. Alternatively, use the Maven view in VS Code to run the project.

## 5. How to use the application
1. Enter a vehicle number (for example `PB10AB1234`) and vehicle type.
2. Click **Park Vehicle**. The system assigns an available slot.
3. The active vehicles table displays the vehicle and its entry time.
4. Select a vehicle in the active table and click **Vehicle Exit**.
5. The app calculates the fee and moves the record to parking history.

## Parking fee rule
The sample rate is **₹20 per started hour**. Any part of an hour is rounded up to the next hour. For example, 1 hour 5 minutes costs ₹40. You can change the rate in `ParkingService.java`.

## Project structure
```text
SmartParkingManagement/
├── database/schema.sql
├── src/main/java/com/student/parking/
│   ├── Main.java
│   ├── DBConnection.java
│   ├── ParkingService.java
│   └── MainFrame.java
├── src/main/resources/db.properties
└── pom.xml
```

## Common problems
- **Access denied for user**: check username/password in `db.properties`.
- **Communications link failure**: make sure MySQL Server is running.
- **Unknown database**: execute `database/schema.sql`.
- **mvn is not recognized**: install Maven and restart VS Code.
- **Unsupported class file / Java version error**: use JDK 17 or newer and confirm `java -version`.

This is an educational starter project. For a real parking facility, add authentication, roles, payment integration, backups, and stronger operational safeguards.
