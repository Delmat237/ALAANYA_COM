package com.alaanya.controller;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.alaanya.database.DatabaseCentral;
import com.alaanya.model.User;
import com.alaanya.socket.Client;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal"})

public class RegisterController {
    @FXML private TextField militaryIdField;
    @FXML private TextField usernameIdField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> gradeCombo;
    @FXML private TextField divisionField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {
        gradeCombo.getItems().addAll(
                "Soldat", "Caporal", "Sergent", "Lieutenant",
                "Capitaine", "Commandant", "Colonel", "Général"
        );
        // Charger la feuille de style
        Scene scene = militaryIdField.getScene();
        if (scene != null) {
            scene.getStylesheets().add(getClass().getResource("/com/alaanya/view/css/style.css").toExternalForm());
        }
    }

    @FXML
    private void handleRegistration() {
        try {
            String militaryId = militaryIdField.getText().toUpperCase();
            Client.userId = militaryId;
            // 1. Validation du format AA-12345 (section 4.1)
            if (!militaryId.matches("[A-Z]{2}-\\d{5}")) {
                throw new IllegalArgumentException("Format ID militaire invalide");
            }

            // 2. Vérification existence utilisateur (section 7 - Sécurité)
            if(User.exists(militaryId)) {
                throw new SQLException("ID militaire déjà enregistré");
            }

            // 3. Vérification complexité mot de passe (section 7)
            if(passwordField.getText().length() < 5) {
                throw new IllegalArgumentException("Mot de passe trop faible (5 caractères minimum)");
            }

            User user = new User(
                    militaryIdField.getText().toUpperCase(), // Normalisation de l'ID
                    gradeCombo.getValue(),
                    divisionField.getText().trim(),
                    calculateClearanceLevel(gradeCombo.getValue()),
                    usernameIdField.getText()
            );

            user.setPasswordHash(User.hashPassword(passwordField.getText()));

            System.out.println("REGISTRATION : " +User.hashPassword(passwordField.getText()));
            if (Client.connectToServer()){
               
                //l'enregistrement se fait dans la bd du serveur distants
                Client.addUserRequest(user.getMilitaryId(),user.getPasswordHash(),user.getGrade(),user.getDivision(),user.getClearanceLevel(),user.getUsername());

            }

            // Ajout du contact par défaut après l'enregistrement réussi de l'utilisateur
            addDefaultContact(militaryId); //ceci sera supprimer ou amelioré

            // Utilisation de la méthode de ViewUtils pour charger la vue principale
            ViewUtils.loadMainView(user.getMilitaryId(), errorLabel);

            errorLabel.setText("Compte créé avec succès !");

        } catch (SQLException | NoSuchAlgorithmException e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    /**
     * Adds a default contact to the user after registration.
     * @param militaryId The military ID of the registered user.
     * @throws SQLException If a database error occurs.
     */
    private void addDefaultContact(String militaryId) throws SQLException {
        String sql = "INSERT INTO contacts (user_id, contact_id) VALUES (?, ?)";
        try (Connection conn = DatabaseCentral.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Ajouter l'ID militaire de l'utilisateur comme contact par défaut (exemple)
            String defaultContactId = "AA-12345"; // ID militaire du contact par défaut
            stmt.setString(1, militaryId);
            stmt.setString(2, defaultContactId);
            stmt.executeUpdate();

            System.out.println("Contact par défaut ajouté pour l'utilisateur : " + militaryId);

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du contact par défaut : " + e.getMessage());
            e.printStackTrace();
            // Gérer l'erreur (par exemple, afficher un message à l'utilisateur)
        }
    }

    private int calculateClearanceLevel(String grade) {
        return switch (grade) {
            case "Général" -> 5;
            case "Colonel" -> 4;
            case "Commandant" -> 3;
            case "Capitaine" -> 2;
            default -> 1;
        };
    }
}
