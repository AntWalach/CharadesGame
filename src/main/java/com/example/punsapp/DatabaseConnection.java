package com.example.punsapp;

import java.sql.Connection;
import java.sql.DriverManager;

// Class for connecting to Oracle Database
public class DatabaseConnection {
    public Connection databaseLink;

    public Connection getConnection() {
        String databaseUser = "sbd32";
        String databasePassword = "sbd32";
        String url = "jdbc:oracle:thin:@149.156.138.232:1521:orcl";

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            databaseLink = DriverManager.getConnection(url, databaseUser, databasePassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return databaseLink;
    }
}

