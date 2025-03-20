package com.alaanya.controller;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import com.alaanya.model.User;
import com.alaanya.socket.Client;
import com.alaanya.socket.Notification;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal","UseSpecificCatch","exports"})

public class LoginController {

    @FXML private TextField militaryIdField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;



    @FXML
    private void handleLogin() {
        String militaryId = militaryIdField.getText();
        String password = passwordField.getText();
        Client.userId = militaryId;

        if (militaryId == null || militaryId.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs.");
            return;
        }

        try {
            AuthenticationResult authResult = authenticateUser(militaryId, password);
            System.out.println(authResult.isAuthenticated());
            if (authResult.isAuthenticated()) {
                System.out.println("next");
                boolean isConnected = Client.connectToServer();
                if (isConnected) {
                    System.out.println("openning view");
                    ViewUtils.loadMainView(militaryId, errorLabel);
                } else {
                    errorLabel.setText("Connexion au serveur Socket échouée.");
                }
            } else {
                errorLabel.setText(authResult.getErrorMessage());
            }
        } catch ( Exception e) {
            errorLabel.setText("Erreur de connexion à la base de données.");
            e.printStackTrace();
        }
    }

    private static class AuthenticationResult {
        private final boolean authenticated;
        private final String errorMessage;

        public AuthenticationResult(boolean authenticated, String errorMessage) {
            this.authenticated = authenticated;
            this.errorMessage = errorMessage;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

//Utilisation de CountDownLatch pour synchroniser l'attente de la réponse
    private AuthenticationResult authenticateUser(String militaryId, String password) throws SQLException, NoSuchAlgorithmException {
        String hashedPassword = User.hashPassword(password);
        System.out.println("auth : " + hashedPassword);

        Client.userId = militaryId;
        AtomicReference<String> errorMessage = new AtomicReference<>("");
        CountDownLatch latch = new CountDownLatch(1); // Permet d'attendre la réponse

        if (!Client.connectToServer()) {
            return new AuthenticationResult(false, "Connexion au serveur central échouée.");
        }

        Client.authUserRequest(militaryId, hashedPassword, notification -> {
            errorMessage.set(notification.getMessage());
            System.out.println("REP " + errorMessage.get());
            latch.countDown(); // Débloque le thread principal
        });

        try {
            latch.await(); // Attend la réponse du serveur avant de continuer
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new AuthenticationResult(false, "Erreur d'attente de réponse du serveur.");
        }

        System.out.println("Final REP " + errorMessage.get());

        if (null == errorMessage.get()) {
            return new AuthenticationResult(false, "Identifiant militaire incorrect.");
        } else switch (errorMessage.get()) {
            case "TRUE" -> {
                System.out.println("on continue");
                return new AuthenticationResult(true, null);
            }
            case "MOT DE PASSE INCORRECT" -> {
                return new AuthenticationResult(false, "Mot de passe incorrect.");
            }
            default -> {
                return new AuthenticationResult(false, "Identifiant militaire incorrect.");
            }
        }
    }

    @FXML
    private void loadRegisterView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alaanya/view/RegisterView.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) militaryIdField.getScene().getWindow();
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onNotificationReceived(Notification notification) {
        Platform.runLater(() -> {
            errorLabel.setText("Notification reçue: " + notification.getMessage());
            if ("alerte".equals(notification.getType())) {
                errorLabel.setStyle("-fx-background-color: red; -fx-text-fill: white;");
            }
        });
    }
}
