package controller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import database.Database;
import model.User;
import socket.Client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal"})

public class RegisterController {
    @ FXML private TextField divisionIdField;
    @FXML private TextField militaryIdField;
    @FXML private TextField usernameIdField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> gradeCombo;
    @FXML private TextField divisionField;
    @FXML private Label errorLabel;

    @FXML
    public void initialize() {

        //Initialisation des grades
        gradeCombo.getItems().addAll(
                "Soldat", "Caporal", "Sergent", "Lieutenant",
                "Capitaine", "Commandant", "Colonel", "Général"
        );
        // Charger la feuille de style
        Scene scene = militaryIdField.getScene();
    }

    @FXML
    private void handleRegistration() {
        try {
            String militaryId = militaryIdField.getText().toUpperCase();
            //Mise à jour de la variable static, userId , permettant de savoir l'utilisateur courant

            Client.userId = militaryId;
            // 1. Validation du format + 2376********
            if (!militaryId.matches("^[+]237[2-9][0-9]{7,8}$")) { //Numero camerounais pour le moment
                throw new IllegalArgumentException("Format du numéro invalide. Exemple : +2376XXXXXXXX");

            }

            // 2. Vérification existence utilisateur (section 7 - Sécurité)
            if(User.exists(militaryId)) {
                throw new SQLException("Numéro déjà enregistré");
            }

            // 3. Vérification complexité mot de passe (section 7)
            if(passwordField.getText().length() < 5) {
                throw new IllegalArgumentException("Mot de passe trop faible (5 caractères minimum)");
            }

            //instanciation d'un utilisateur

            User user = new User(
                    militaryIdField.getText(), 
                    gradeCombo.getValue(),
                    divisionField.getText().trim(),
                    usernameIdField.getText()
            );

            //AJout du mot de passe haché en avance
            user.setPasswordHash(User.hashPassword(passwordField.getText()));

            System.out.println("REGISTRATION : " +User.hashPassword(passwordField.getText()));

            //verification prealable si une connection peut etre etablit avec le serveur centrale
            if (Client.connectToServer()){
               
                //l'enregistrement se fait dans la bd du serveur distants et en local  si tous se passe bien du coté serveur centrale
                Client.addUserRequest(user.getPhone_Number(),user.getPasswordHash(),user.getGrade(),user.getDivision(),user.getUsername());

                //Enregistrer l'user en local
                Database.addUser(user);

            }

            // Utilisation de la méthode de ViewUtils pour charger la vue principale
            errorLabel.setText("Compte créé avec succès !");

            //Renvoyer sur la page de Login
            loadLoginView();
            // ViewUtils.loadMainView(user.getPhone_Number(), errorLabel,1);

        } catch (SQLException | NoSuchAlgorithmException e) {
            errorLabel.setText("Erreur : " + e.getMessage());
        }
    }

    @FXML
    private void loadLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alaanya/view/LoginView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) militaryIdField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void postInit() {
        Scene scene = militaryIdField.getScene();
        if (scene != null) {
            scene.getStylesheets().add(getClass().getResource("/com/alaanya/view/css/style.css").toExternalForm());
        }
    }


}
