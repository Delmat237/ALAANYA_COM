package com.alaanya.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.alaanya.database.Database;

@SuppressWarnings("CallToPrintStackTrace")   


public class Contact {

    public static String getContactsFromDatabase(String militaryId, List<String> contactsList) throws SQLException {
        System.out.println("recherche des contacts");

        String sql = "SELECT u.username, u.division, u.military_id " + // Ajouter u.military_id
                "FROM users u " +
                "INNER JOIN contacts c ON u.military_id = c.contact_id " +
                "WHERE c.user_id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, militaryId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String username = rs.getString("username");
                String division = rs.getString("division");
                String contactId = rs.getString("military_id"); // Récupérer l'ID militaire
               
                return (username + " (" + division + " - " + contactId + ")"); // Ajouter l'ID militaire à la chaîne
            }
        }
                return null;
    }

    /**
     * Ajoute un contact à la base de données.
     * @param userId L'ID militaire de l'utilisateur actuel.
     * @param contactId L'ID militaire du contact à ajouter.
     * @throws SQLException Si une erreur de base de données se produit.
     */

     public static void addContactToDatabase(String userId, String contactId) {
        if (!userExists(contactId)) {
                    System.err.println("[SERVER] Contact ID " + contactId + " does not exist in the users table.");
                    // Handle the error appropriately.  For example, show an error message to the user.
                    return; // Do not proceed with the insertion
                }
        
                String sql = "INSERT INTO contacts (user_id, contact_id) VALUES (?, ?)";
        
                //aPRES je vais retirer ca ici (Factory)
                try (Connection conn = Database.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
        
                    stmt.setString(1, userId);
                    stmt.setString(2, contactId);
        
                    stmt.executeUpdate();
                    System.out.println("[SERVER] Contact added: " + userId + " -> " + contactId);
        
                    // Update the contact list
                    //loadContacts();
        
                } catch (SQLException e) {
                    System.err.println("[SERVER] Error adding contact: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        
            // Helper method to check if a user exists in the users table
         private static boolean userExists(String militaryId) {
        String sql = "SELECT 1 FROM users WHERE military_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, militaryId);
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // Returns true if a user with the given militaryId exists
        } catch (SQLException e) {
            System.err.println("[SERVER] Error checking if user exists: " + e.getMessage());
            e.printStackTrace();
            return false; // Assume user doesn't exist in case of an error
        }
    }
    
}
