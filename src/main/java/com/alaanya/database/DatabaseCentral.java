package com.alaanya.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal"})

public class DatabaseCentral {
    private static final String DB_PROPERTIES_FILE = "dbCentral.properties";
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    static {
        System.out.println("[DatabaseCentral] Début de l'initialisation...");

        Properties props = new Properties();
        try (InputStream input = DatabaseCentral.class.getClassLoader().getResourceAsStream(DB_PROPERTIES_FILE)) {
            if (input == null) {
                throw new IOException("Fichier de configuration non trouvé: " + DB_PROPERTIES_FILE);
            }
            props.load(input);
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASSWORD = props.getProperty("db.password");
        } catch (IOException ex) {
            System.err.println("[DatabaseCentral] Erreur chargement fichier db.properties : " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("Erreur lors du chargement du fichier de configuration", ex);
        }

        // Vérifier la connexion à la BD
        try (Connection testConn = DriverManager.getConnection(URL, USER, PASSWORD)) {
        System.out.println("[DatabaseCentral] Configuration chargée : URL=" + URL);

        // Vérifier la connexion à la BD
        } catch (SQLException e) {
            System.err.println("[DatabaseCentral] Échec de connexion à la base de données : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Impossible de se connecter à la base de données", e);
        }

        // Création de la BD et des tables
        try {
            System.out.println("[DatabaseCentral] Création de la base de données et des tables...");
            createDatabaseAndTable();
            System.out.println("[DatabaseCentral] Base de données et tables créées avec succès !");
        } catch (SQLException e) {
            System.err.println("[DatabaseCentral] Erreur création BD et tables : " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la création de la base de données et des tables", e);
        }
    }


    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static void createDatabaseAndTable() throws SQLException {
        String dbName = "alaanya_db_central";
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306?allowMultiQueries=true&useSSL=false&serverTimezone=UTC", USER, PASSWORD);
             Statement stmt = conn.createStatement()) {

            // Création de la base de données si elle n'existe pas
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);

            // Connexion à la base de données
            try (Connection dbConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/" + dbName + "?useSSL=false&serverTimezone=UTC", USER, PASSWORD);
                 Statement dbStmt = dbConn.createStatement()) {

                // Création de la table users
                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                        "military_id VARCHAR(8) PRIMARY KEY," +
                        "password_hash VARCHAR(64) NOT NULL," +
                        "grade VARCHAR(50) NOT NULL," +
                        "division VARCHAR(50) NOT NULL," +
                        "clearance_level INT NOT NULL," +
                        "username VARCHAR(255) NOT NULL" +
                        ")");

                // Création de la table addressTable
                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS addressTable (" +
                        "user_id VARCHAR(8) NOT NULL," +
                        "contact_id VARCHAR(8) NOT NULL," +
                        "PRIMARY KEY (user_id, contact_id)," +
                        "FOREIGN KEY (user_id) REFERENCES users(military_id)," +
                        "FOREIGN KEY (contact_id) REFERENCES users(military_id)" +
                        ")");

                // Création de la table messages
                dbStmt.executeUpdate("CREATE TABLE IF NOT EXISTS messages (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "sender VARCHAR(8) NOT NULL," +
                        "recipient VARCHAR(8) NOT NULL," +
                        "content TEXT NOT NULL," +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (sender) REFERENCES users(military_id)," +
                        "FOREIGN KEY (recipient) REFERENCES users(military_id)" +
                        ")");
            }
        }
    }
}
