package com.alaanya.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import com.alaanya.model.User;

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
                dbStmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS users (\
                    military_id VARCHAR(8) PRIMARY KEY,\
                    password_hash VARCHAR(64) NOT NULL,\
                    grade VARCHAR(50) NOT NULL,\
                    division VARCHAR(50) NOT NULL,\
                    clearance_level INT NOT NULL,\
                    username VARCHAR(255) NOT NULL\
                    )""");

                // Création de la table addressTable
                dbStmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS addressesTable (\
                    user_id VARCHAR(8) NOT NULL,\
                    user_address VARCHAR(8) NOT NULL)""");

                // Création de la table messages
                dbStmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS messages (\
                    id INT AUTO_INCREMENT PRIMARY KEY,\
                    sender VARCHAR(8) NOT NULL,\
                    recipient VARCHAR(8) NOT NULL,\
                    content TEXT NOT NULL,\
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\
                    FOREIGN KEY (sender) REFERENCES users(military_id),\
                    FOREIGN KEY (recipient) REFERENCES users(military_id)\
                    )""");
            }
        }
    }

     public static void addUser(User user) throws SQLException{
        //Methode permettant d'ajouter les utilisateur
        String sql = "INSERT INTO users (military_id, password_hash, grade, division, clearance_level, username) VALUES (?, ?, ?, ?, ?, ?)";

                            try (Connection conn = DatabaseCentral.getConnection();
                                PreparedStatement stmt = conn.prepareStatement(sql)) {
                                
                                stmt.setString(1, user.getMilitaryId());
                                stmt.setString(2, user.getPasswordHash());
                                stmt.setString(3, user.getGrade());
                                stmt.setString(4, user.getDivision());
                                stmt.setInt(5, user.getClearanceLevel());
                                stmt.setString(6, user.getUsername());  // Ajout du nom d'utilisateur

                                stmt.executeUpdate();
 
                }
        }

        public static String getUser(String militaryId) throws SQLException{
            String sql = "SELECT military_id, grade, division, clearance_level, username FROM users WHERE military_id = ?";
            Connection conn = DatabaseCentral.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
              stmt.setString(1, militaryId);
            
              String msg = null;
              ResultSet rs = stmt.executeQuery();

             if (rs.next()) {
                                    //preparation du message (info du user)
                                 msg =
                                            rs.getString("military_id")+"&&"+
                                            rs.getString("grade")+"&&"+
                                            rs.getString("division")+"&&"+
                                            rs.getInt("clearance_level")+"&&"+
                                            rs.getString("username");

                                
                                    System.out.println("[CENTRAL SERVER] getting success (user found) for: " + militaryId);
              } else {
                                    System.out.println("[CENTRAL SERVER] getting FAILED (user not found) for: " + militaryId);
                                    

             }
                                return msg;
        }

        public static String authUser(String militaryId, String password) throws SQLException{
            Connection conn = DatabaseCentral.getConnection();
              //Ajout de l'user dans la table d'addressage
             String sql0 = "INSERT INTO addressesTable (military_id, ip_address) VALUES (?,?)";
            PreparedStatement stmt0 = conn.prepareStatement(sql0);

            String sql = "SELECT * FROM users WHERE military_id = ?";
             PreparedStatement stmt = conn.prepareStatement(sql);
             stmt.setString(1, militaryId);

          ResultSet rs = stmt.executeQuery();

              if (rs.next()) {
                                    String storedPasswordHash = rs.getString("password_hash");

                  if (storedPasswordHash.equals(password)) {
                                   System.out.println("[CENTRAL SERVER] Authentication successful for user: " + militaryId);
                                    return "AUTH_SUCCESS&&TRUE";
                                       
                } else {
                                    System.out.println("[CENTRAL SERVER] Authentication failed (wrong password) for user: " + militaryId);
                                     return "AUTH_FAILURE&&MOT DE PASSE INCORRECT";
                 }
           } else {
                        System.out.println("[CENTRAL SERVER] Authentication failed (user not found) for: " + militaryId);
                        return "AUTH_FAILURE&&IDENTIFIANT MILITAIRE INCORRECT";
                 
          }
            
        }

}
