package com.alaanya.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Database {
    private static final String DB_PROPERTIES_FILE = "db.properties";
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = Database.class.getClassLoader().getResourceAsStream(DB_PROPERTIES_FILE);
            if (input == null) {
                System.err.println("Unable to find " + DB_PROPERTIES_FILE);
                throw new IOException("Unable to find " + DB_PROPERTIES_FILE);
            }
            props.load(input);
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");
        } catch (IOException ex) {
            System.err.println("Erreur lors du chargement du fichier de configuration : " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Erreur lors du chargement du fichier de configuration", ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Créer la base de données et la table si elles n'existent pas
        try {
            createDatabaseAndTable();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la base de données et de la table : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la création de la base de données et de la table", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static void createDatabaseAndTable() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306?allowMultiQueries=true", USER, PASSWORD); // Connexion sans base de données spécifiée
             Statement stmt = conn.createStatement()) {

            // Créer la base de données si elle n'existe pas
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS alaanya_db");

            // Utiliser la base de données
            stmt.executeUpdate("USE alaanya_db");

            // Créer la table users si elle n'existe pas
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                    "military_id VARCHAR(8) PRIMARY KEY," +
                    "password_hash VARCHAR(64) NOT NULL," +
                    "grade VARCHAR(50) NOT NULL," +
                    "division VARCHAR(50) NOT NULL," +
                    "clearance_level INT NOT NULL," +
                    "username VARCHAR(255) NOT NULL" +
                    ")");
            //Créer la table contacts si elle n"existe pas
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS contacts (\n" +
                    "    user_id VARCHAR(8) NOT NULL,\n" +
                    "    contact_id VARCHAR(8) NOT NULL,\n" +
                    "    PRIMARY KEY (user_id, contact_id),\n" +
                    "    FOREIGN KEY (user_id) REFERENCES users(military_id),\n" +
                    "    FOREIGN KEY (contact_id) REFERENCES users(military_id)\n" +
                    ");\n");

            //Créer la table message
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS messages (\n" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY,\n" +
                    "    sender VARCHAR(8) NOT NULL,\n" +
                    "    recipient VARCHAR(8) NOT NULL,\n" +
                    "    content TEXT NOT NULL,\n" +
                    "    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                    "    FOREIGN KEY (sender) REFERENCES users(military_id),\n" +
                    "    FOREIGN KEY (recipient) REFERENCES users(military_id)\n" +
                    ");\n");
        }
    }
}
