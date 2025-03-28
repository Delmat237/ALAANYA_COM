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
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (\
                military_id VARCHAR(8) PRIMARY KEY,\
                password_hash VARCHAR(64) NOT NULL,\
                grade VARCHAR(50) NOT NULL,\
                division VARCHAR(50) NOT NULL,\
                clearance_level INT NOT NULL,\
                username VARCHAR(255) NOT NULL\
                )""");
            //Créer la table contacts si elle n"existe pas
            stmt.executeUpdate("""
                               CREATE TABLE IF NOT EXISTS contacts (
                                   user_id VARCHAR(8) NOT NULL,
                                   contact_id VARCHAR(8) NOT NULL,
                                   PRIMARY KEY (user_id, contact_id),
                                   FOREIGN KEY (user_id) REFERENCES users(military_id),
                                   FOREIGN KEY (contact_id) REFERENCES users(military_id)
                               );
                               """);

            //Créer la table message
            stmt.executeUpdate("""
                               CREATE TABLE IF NOT EXISTS messages (
                                   id INT AUTO_INCREMENT PRIMARY KEY,
                                   sender VARCHAR(8) NOT NULL,
                                   recipient VARCHAR(8) NOT NULL,
                                   content TEXT NOT NULL,
                                   timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   FOREIGN KEY (sender) REFERENCES users(military_id),
                                   FOREIGN KEY (recipient) REFERENCES users(military_id)
                               );
                               """);
        }
    }

    public static void addUser(User user) throws SQLException{
        //Methode permettant d'ajouter les utilisateur
        String sql = "INSERT INTO users (military_id, password_hash, grade, division, clearance_level, username) VALUES (?, ?, ?, ?, ?, ?)";

                            try (Connection conn = Database.getConnection();
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

 public static String authUser(String militaryId, String password) throws SQLException{
            Connection conn = Database.getConnection();
         
            String sql = "SELECT * FROM users WHERE military_id = ?";
             PreparedStatement stmt = conn.prepareStatement(sql);
             stmt.setString(1, militaryId);

          ResultSet rs = stmt.executeQuery();

              if (rs.next()) {
                                    String storedPasswordHash = rs.getString("password_hash");

                  if (storedPasswordHash.equals(password)) {
                                   System.out.println("[LOCALHOST] Authentication successful for user: " + militaryId);
                                    return "AUTH_SUCCESS&&TRUE";
                                       
                } else {
                                    System.out.println("[LOCALHOST] Authentication failed (wrong password) for user: " + militaryId);
                                     return "AUTH_FAILURE&&MOT DE PASSE INCORRECT";
                 }
           } else {
                        System.out.println("[LOCALHOST] Authentication failed (user not found) for: " + militaryId);
                        return "AUTH_FAILURE&&IDENTIFIANT MILITAIRE INCORRECT";
                 
          }
            
        }

        public static User getUser(String militaryId) throws SQLException{
            String sql = "SELECT military_id, grade, division, clearance_level, username FROM users WHERE military_id = ?";
            Connection conn = DatabaseCentral.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
              stmt.setString(1, militaryId);
            
              String msg = null;
              ResultSet rs = stmt.executeQuery();

             if (rs.next()) {
                                System.out.println("[LOCALHOST] getting success (user found) for: " + militaryId);
                                
                                return new User( rs.getString("military_id"), rs.getString("grade"), 
                                rs.getString("division"),rs.getInt("clearance_level"),   rs.getString("username"));
                                  
              } else {
                                    System.out.println("[LOCALHOST] getting FAILED (user not found) for: " + militaryId);
                                    return null;
                                    

             }
                               
        }

}
