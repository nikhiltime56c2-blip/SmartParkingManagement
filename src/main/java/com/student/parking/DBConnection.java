package com.student.parking;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public final class DBConnection {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("db.properties was not found in src/main/resources.");
            }
            PROPERTIES.load(input);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private DBConnection() {
    }

    public static Connection getConnection() throws Exception {
        String url = PROPERTIES.getProperty("db.url");
        String user = PROPERTIES.getProperty("db.user");
        String password = PROPERTIES.getProperty("db.password");
        return DriverManager.getConnection(url, user, password);
    }
}
