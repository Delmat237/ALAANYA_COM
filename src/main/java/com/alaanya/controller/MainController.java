package com.alaanya.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.alaanya.MainApp;
import com.alaanya.database.Database;
import com.alaanya.model.Contact;
import com.alaanya.model.User;
import com.alaanya.socket.Client;
import com.alaanya.socket.FileMessage;
import com.alaanya.socket.Message;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal","exports"})

public class MainController {

    @FXML private Label userLabel;
    @FXML private Label gradeLabel;
    @FXML private Label divisionLabel;
    @FXML private Label idLabel;
    @FXML private Label statut;
    @FXML private Label selectedUserLabel;
    @FXML private ListView<String> contactListView;
    @FXML private TextArea chatTextArea;
     @FXML private VBox chatVBox;
    @FXML private TextField messageTextField;
    @FXML private TextField searchTextField;
    @FXML private ListView<String> searchResultsListView;
    @FXML private VBox defaultCenterVBox;
    @FXML private VBox chatAreaVBox;
   

    private User user;
    private static String recipientAddress;
    private ObservableList<String> contacts = FXCollections.observableArrayList();
    private ObservableList<String> contactList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // observe contactListView
        contactListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        setSelectedUsername(newValue); // Set the username in the label
                        showChatArea(true); // Show chat area when contact is selected
                        String userId =  extractContactIdFromContactList(newValue.substring(0, newValue.length() - 1));

                        //envoie une requete de recupperation d'addresse IP
                        Client.requestAddress(userId, notification -> {
                            Platform.runLater(() -> {
                                if ("ADDRESS_RESPONSE".equals(notification.getType())) {
                                    recipientAddress = notification.getMessage();
                                    System.out.println("[CLIENT] Address for " + newValue + ": " + recipientAddress);
                                    // Use this address for further actions like starting an audio call
                                } else if ("ADDRESS_NOT_FOUND".equals(notification.getType())) {
                                    System.err.println("[CLIENT] Address not found for user: " + newValue);
                                }
                            });
                        });
                    } else {
                        showChatArea(false); // Hide chat area when no contact is selected
                    }
                }
        );
        // Initially hide the chat area
        showChatArea(false);
    }

    public void setSelectedUsername(String username) {
        selectedUserLabel.setText(username);
    }

    private void showChatArea(boolean show) {
        chatAreaVBox.setVisible(show);
        defaultCenterVBox.setVisible(!show);
    }

    public void setUser(User user,int state) {
        this.user = user;
        Platform.runLater(() -> {
            userLabel.setText(user.getUsername());
            gradeLabel.setText("Grade: " + user.getGrade());
            divisionLabel.setText("Division: " + user.getDivision());
            idLabel.setText("Identifiant Militaire: " + user.getMilitaryId());
           if (state == 1)
            statut.setText("STATE :online");
           else
           statut.setText("STATE : disconnected");

            try {
                contacts.clear();
            
                //Recupération des contacts de l'utilisateur
                contactList.add(Database.getContacts(user.getMilitaryId(), contacts));

                contactListView.setItems(contactList);

                contactListView.setOnMouseClicked(event -> {

                    String selectedContact = contactListView.getSelectionModel().getSelectedItem(); //RECUPERATION DU CONTACT SELECTIONNÉ
                    if (selectedContact != null) {
                        String contactId = extractContactIdFromContactList(selectedContact.substring(0, selectedContact.length() - 1)); // Utiliser la méthode extractContactIdFromContactList
                        System.out.println("lE CONTACT SELECTIONNÉ EST " + contactId);
                        //Envoie une requete au serveur pour recuperer l'adress IP du contact selectionné

                        loadConversation(user.getMilitaryId(), contactId); //Recuperation des messages
                    }
                });
            } catch (SQLException e) {
                System.err.println("Erreur lors de la récupération des contacts : " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    public User getUser() {
        return user;
    }

    /**
     * Ajoute un contact à la liste de contacts de l'utilisateur.
     */
    @FXML
    private void addContact() {
        String selectedContact = searchResultsListView.getSelectionModel().getSelectedItem(); //RECUPERE LE CONTACT SELECTIONNÉ
        System.out.println("Ajout de "+selectedContact);
        if ((selectedContact = extractContactIdFromContactList(selectedContact.substring(0, selectedContact.length() - 1))) != null) {
            // Remove the extra parenthesis if it exists
            if (selectedContact.endsWith(")")) {
                selectedContact = selectedContact.substring(0, selectedContact.length() - 1);
            }
            String contactId = selectedContact.trim(); // Remove leading/trailing whitespace
            System.out.println("Le contact de la personne est "+contactId);

            // Now add the contact to the database and contact list view
            if (user!= null) {
                contactListView.getItems().add(user.getUsername());

                //Recupere l'id de l'utilisateur
                String userId = user.getMilitaryId();
                System.out.println("Table de conatact "+ userId + ": "+ contactId);
                try{
                    //Enregistre le contact dans la BD
                    Database.addContact(userId,contactId,0,"New Contact");

                } catch(Exception e){
                    System.out.println(e.getMessage());
                }

            } else {
                System.err.println("[ERROR] No user logged in");
                //Handle the error by notifying the user that they must log in to add
            }

        } else {
            System.err.println("[ERROR] No contact selected");
            //Handle the error by notifiying the user that they did not select the text
        }
    }

    /**
     * Sends a message to the selected contact.
     */
    @FXML
    private void sendMessage() {
        String messageText = messageTextField.getText(); //recuperation du message saisir
        String selectedContact = contactListView.getSelectionModel().getSelectedItem(); //recuperation de l'ID du destinataire

        if (!messageText.isEmpty() && selectedContact != null) {
            // Extraire l'ID du contact sélectionné en utilisant la méthode appropriée
            String recipientId = extractContactIdFromContactList(selectedContact.substring(0, selectedContact.length() - 1));

            if (recipientId != null) {
                Message message = new Message(user.getMilitaryId(), "MESSAGE",messageText, recipientId);
                //Recupération de l'address IP actuell du destinataire
                //LOGIQUE ICI


                //RecipentAddress
                Client.sendMessage(message,recipientAddress); // Envoi du message via la classe Client

                //Ajout du message dans la zone de chat
                MessageController.addMessage(chatVBox,user.getMilitaryId(),message.getContent(),true);

                messageTextField.clear();
            } else {
                System.out.println("Erreur : Impossible d'extraire l'ID du contact.");
                // Afficher un message d'erreur à l'utilisateur
            }
        } else {
            System.out.println("Veuillez sélectionner un contact et saisir un message.");
        }
    }

    // Method to select a file and send it
    @FXML
    private void sendFile() {
        if (user != null) {
            String sender = user.getMilitaryId(); //recupere l'ID du user
            String selectedContact = contactListView.getSelectionModel().getSelectedItem();
            // Extraire l'ID du contact sélectionné en utilisant la méthode appropriée
            String recipientId = extractContactIdFromContactList(selectedContact.substring(0, selectedContact.length() - 1));
            System.out.println(recipientId);
            sendFile(sender, recipientId); // Call the sendFile method
        } else {
            System.err.println("No user logged in");
        }
    }
    private void sendFile(String sender,String recipientId) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());
                String fileName = selectedFile.getName();
                FileMessage fileMessage = new FileMessage(sender, "FILE_UPLOAD", "file upload", fileName, fileData,recipientId);

                Client.sendMessage(fileMessage,recipientAddress); // Use your sendMessage method to send the file

                //Ajout du fichier dans la zone de chat

                FileController.addFile(chatVBox,"com/alaanya/view/images/file.png",fileName,true);

                System.out.println("[CLIENT] Sending file: " + fileName + " (" + fileData.length + " bytes)");
            } catch (IOException e) {
                System.err.println("[CLIENT] Error reading file: " + e.getMessage());
            }
        } else {
            System.out.println("[CLIENT] File selection cancelled.");
        }
    }


/**
     * Loads the conversation between the current user and the selected contact.
     * @param currentUserId The military ID of the current user.
     * @param contactId The military ID of the selected contact.
     */
    private void loadConversation(String currentUserId, String contactId) {
        try {

            List<Message> conversation = getConversationFromDatabase(currentUserId, contactId);

            for (Message message : conversation) {
                String sender = message.getSender().equals(currentUserId) ? "Vous" : message.getSender();
                System.out.println("msg lu :" +  message.getContent());

                MessageController.addMessage(chatVBox,sender,message.getContent(),sender.equals("Vous")?true:false);
              
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement de la conversation : " + e.getMessage());
            e.printStackTrace();
            // Afficher un message d'erreur à l'utilisateur
        }
    }

    /**
     * Retrieves the conversation between two users from the database.
     * @param currentUserId The military ID of the current user.
     * @param contactId The military ID of the selected contact.
     * @return A list of Message objects representing the conversation.
     * @throws SQLException If a database error occurs.
     */
    private List<Message> getConversationFromDatabase(String currentUserId, String contactId) throws SQLException {
        List<Message> conversation = new ArrayList<>();
        String sql = "SELECT sender, content FROM messages " +
                "WHERE (sender = ? AND recipient = ?) OR (sender = ? AND recipient = ?) " +
                "ORDER BY timestamp";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentUserId);
            stmt.setString(2, contactId);
            stmt.setString(3, contactId);
            stmt.setString(4, currentUserId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Message message = new Message(
                        rs.getString("sender"),
                        "MESSAGE",
                        rs.getString("content")

                );
                conversation.add(message);
            }
        }
        return conversation;
    }

    @FXML
    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/alaanya/view/LoginView.fxml"));
            GridPane loginView = (GridPane) loader.load();

            Scene scene = new Scene(loginView);
            Stage stage = MainApp.getPrimaryStage();
            stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extracts the contact ID from the contact list string format.
     * @param contactString The string representation of the contact in the list.
     * @return The contact ID, or null if it cannot be extracted.
     */
    private String extractContactIdFromContactList(String contactString) {
        if (contactString != null && contactString.contains(" - ")) {
            return contactString.substring(contactString.lastIndexOf(" - ") + 3).trim();
        }
        return null;
    }

    /**
     * Extracts the contact ID from the search results list string format.
     * @param resultString The string representation of the search result.
     * @return The contact ID, or null if it cannot be extracted.
     */
    private String extractContactIdFromResultList(String resultString) {
        if (resultString != null && resultString.contains(" - ")) {
            return resultString.substring(resultString.lastIndexOf(" - ") + 3).trim();
        }
        return null;
    }



    public void sendMessageKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            sendMessage();
        }
    }

    /*
    Methode relative à la recherche de conatact
     */

    @FXML
    private void handleSearchKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            System.out.println("Recherche d'un contact");
            searchUsers();
        }
    }
    @FXML
    public void searchContacts(ActionEvent event) {
        System.out.println("Recherche d'un contact");
        searchUsers();
    }
    /**
     * Recherche des utilisateurs par ID militaire ou nom d'utilisateur.
     */
    @FXML
    private void searchUsers() {
        String searchTerm = searchTextField.getText();
        if (!searchTerm.isEmpty()) {
            try {
                List<String> searchResultsList = searchUsersInDatabase(searchTerm);
                System.out.println("Ajout des contacts trouvé , j'ai trouvé "+searchResultsList.size());

                searchResultsListView.getItems().addAll(searchResultsList);
            } catch (SQLException e) {
                System.err.println("Erreur lors de la recherche d'utilisateurs : " + e.getMessage());
                e.printStackTrace();
                // Afficher un message d'erreur à l'utilisateur
            }
        }
    }
    /**
     * Effectue la recherche d'utilisateurs dans la base de données.
     * @param searchTerm Le terme de recherche saisi par l'utilisateur.
     * @return Une liste de chaînes de caractères représentant les résultats de la recherche.
     * @throws SQLException Si une erreur de base de données se produit.
     */
    private List<String> searchUsersInDatabase(String searchTerm) throws SQLException {
        List<String> searchResultsList = new ArrayList<>();
        /*
         * l'acces a la bd ne doit pas se faire ici*/
        String sql = "SELECT military_id, username, division FROM users " +
                "WHERE military_id LIKE ? OR username LIKE ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + searchTerm + "%");
            stmt.setString(2, "%" + searchTerm + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String militaryId = rs.getString("military_id");
                String username = rs.getString("username");
                String division = rs.getString("division");
                searchResultsList.add(username + " (" + division + " - " + militaryId + ")");
            }
        }
        return searchResultsList;
    }
}
