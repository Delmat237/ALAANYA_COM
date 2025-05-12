package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import audio.AudioSetup;
import com.alaanya.MainApp;

import database.Database;
import file.FileReceiver;
import file.FileSender;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import message.MessageReceiver;
import message.MessageSender;
import model.Contact;
import model.Message;
import model.User;
import signal.CallSignaler;
import socket.Client;
import utils.SoundPlayer;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal","exports"})

public class MainController {


    @FXML
    private TabPane contactsTabPane;
    @FXML
    private Label userLabel;
    @FXML
    private Label gradeLabel;
    @FXML
    private Label divisionLabel;
    @FXML
    private Label idLabel;
    @FXML
    private Label statutLabel;
    @FXML
    private Label selectedUserLabel;
    @FXML
    private Label selectedUserStatut;
    @FXML
    private TextArea chatTextArea;
    @FXML
    public VBox chatVBox;
    @FXML
    private TextField messageTextField;
    @FXML
    private TextField searchTextField;
    @FXML
    private VBox defaultCenterVBox;
    @FXML
    private VBox chatAreaVBox;
    @FXML
    private VBox settingsVBox;
    @FXML
    private Label settingsUserLabel;
    @FXML
    private Label settingsGradeLabel;
    @FXML
    private Label settingsDivisionLabel;
    @FXML
    private Label settingsIdLabel;
    @FXML
    private TitledPane addContactPane;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextField nicknameField;
    @FXML
    private ListView<Contact> contactListView; //LISTE DE CONTACT QUI SERA AFFICHÉ
    @FXML
    private ListView<Contact> allContactsListView;


    private User user;
    private static String recipientAddress;

    private ObservableList<Contact> messageContacts = FXCollections.observableArrayList();
    private ObservableList<Contact> allContacts = FXCollections.observableArrayList();
    private Contact selectedContact;
    private final AudioSetup audioSetup = new AudioSetup();

    static int FILE_PORT = 5001;
    static int MESSAGE_PORT = 5000;
    public static final int AUDIO_PORT = 5002;
    public static final int SIGNAL_PORT = 5003;
    static int VIDEO_PORT = 5004;

    private int seconds = 0;
    private Timeline callTimer;

    @FXML
    public void initialize() throws SQLException {

        SoundPlayer.playSound("/sounds/start.wav");

        //THread de reception des messages texte
        MessageReceiver receiver = new MessageReceiver(MESSAGE_PORT);
        receiver.setMessageListener(message -> {
            handleIncomingMessage(message);
            SoundPlayer.playSound("/sounds/not.wav");
        });

        receiver.start();


        //Thread de reception des fichiers
        new FileReceiver(FILE_PORT).start();

        CallSignaler signaler = new CallSignaler();
        signaler.listenForCallRequests(new CallSignaler.CallListener() {
            @Override
            public void onCallReceived(String fromUser, String ip) {
                Platform.runLater(() -> {
                    boolean accepted = showConfirmationDialog("Appel de " + fromUser);
                    signaler.sendCallResponse(ip, accepted);
                    if (accepted) {
                        openAudioChat(ip, fromUser);
                    }
                });
            }

            @Override
            public void onCallAccepted(String ip) {
                System.out.println("Appel accepté !");
            }

            @Override
            public void onCallDeclined(String ip) {
                System.out.println("Appel refusé.");
            }
        });

        // Filtrage en mémoire pour "Mes contacts"
        searchTextField.textProperty().addListener((obs, oldValue, newValue) -> {
            filterContacts(newValue);
        });

        //Personnalisation des liste des contacts
        ContactListView(contactListView);
        ContactListView(allContactsListView);


        showChatArea(false);
    }

    public void ContactListView(ListView<Contact> listView) {
        //Personnalisation de l'affichage des contacts
        listView.setCellFactory(list -> new ListCell<Contact>() {
            private final HBox content;
            private final ImageView imageView;
            private final VBox texts;
            private final Label name;
            private final Label lastMessage;
            private final Label date;
            private final Label unreadCount;

            {
                Image image = new Image(getClass().getResource("/com/alaanya/view/images/profile.png").toExternalForm());
                imageView = new ImageView(image);
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setClip(new Circle(20, 20, 20)); // rond

                name = new Label();
                name.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

                lastMessage = new Label();
                lastMessage.setStyle("-fx-text-fill: gray;");

                date = new Label();
                date.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10;");
                unreadCount = new Label();
                unreadCount.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-padding: 2 5 2 5; -fx-background-radius: 10;");
                unreadCount.setVisible(false);

                texts = new VBox(name, lastMessage);
                HBox rightBox = new HBox(date, unreadCount);
                rightBox.setSpacing(10);
                rightBox.setAlignment(Pos.TOP_RIGHT);

                content = new HBox(imageView, texts, rightBox);
                content.setSpacing(10);
                content.setPadding(new Insets(5));
                content.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(texts, Priority.ALWAYS);
            }

            //Methode de mise à jour
            protected void updateItem(Contact item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    imageView.setImage(item.getProfileImage());
                    name.setText(item.getName());
                    lastMessage.setText(item.getLastMessage());
                    date.setText(item.getDate());
                    if (item.getUnreadCount() > 0) {
                        unreadCount.setText(String.valueOf(item.getUnreadCount()));
                        unreadCount.setVisible(true);
                    } else {
                        unreadCount.setVisible(false);
                    }
                    setGraphic(content);
                }
            }
        });

        //Action que s'execute lorsqu'on clique sur un contact
        listView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (newValue != null) {
                        selectedContact = newValue;
                        setSelectedUsername(newValue.getName());
                        showChatArea(true);

                        String userId = newValue.getPhone_number();
                        //charge les conversation
                        loadConversation(user.getPhone_Number(), userId);

                        Client.requestAddress(userId, notification -> {
                            Platform.runLater(() -> {
                                if ("ADDRESS_RESPONSE".equals(notification.getType())) {
                                    recipientAddress = notification.getMessage();
                                    selectedUserStatut.setText("online");
                                } else if ("ADDRESS_NOT_FOUND".equals(notification.getType())) {

                                    Alert alert = new Alert(Alert.AlertType.ERROR, " Address not found for user: " + newValue, ButtonType.OK);
                                    alert.showAndWait();

                                    selectedUserStatut.setText("offline");
                                }
                            });
                        });
                    } else {
                        showChatArea(false);
                    }
                });

    }

    private void handleIncomingMessage(Message message) {
        Platform.runLater(() -> {
            // Envoyer une confirmation de lecture si le message recu n'est pas deja une confirmation de lecture
            if (!Objects.equals(message.getType(), "ACK_READ")) {
                //affiche une notification
                if(!Objects.equals(message.getRecipient(), selectedContact.getPhone_number()))
                    showInfo("Nouveau message de "+message.getSender(),message.getContent());

                //Mise à jour du dernier message
                try {
                    Database.updateContact(message);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                if (Objects.equals(message.getType(), "MESSAGE"))
                    MessageController.addMessage(chatVBox, message.getSender(), message.getContent(), false, "read");
                else
                    FileController.addFile(chatVBox, message.getSender(), message.getContent(), false);
                Message readAck = new Message(user.getPhone_Number(), "ACK_READ", "read", message.getSender());
                new MessageSender(recipientAddress, MESSAGE_PORT, readAck).start();

                // Mettre à jour dans la base de données
                try {
                    Database.updateMessageStatus(message, "read");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }

        });
    }

    public void setSelectedUsername(String username) {
        selectedUserLabel.setText(username);
    }

    private void showChatArea(boolean show) {
        chatAreaVBox.setVisible(show);
        defaultCenterVBox.setVisible(!show);
    }

    public void setUser(User user, int state) throws SQLException {
        this.user = user;

        // Récupère les données depuis la base
        messageContacts.setAll(Database.getContactsWithMessages(user.getPhone_Number()));
        allContacts.setAll(Database.getAllContacts());

        contactListView.setItems(messageContacts);
        allContactsListView.setItems(allContacts);
        userLabel.setText(user.getUsername());
        gradeLabel.setText(user.getGrade());
        divisionLabel.setText(user.getDivision());
        idLabel.setText(user.getPhone_Number());
        statutLabel.setText((state == 1) ? "online" : "offline");


    }

    public User getUser() {
        return user;
    }

    /**
     * Ajoute un contact à la liste de contacts de l'utilisateur.
     */
    @FXML
    private void addContact() throws SQLException {
        String phone = phoneNumberField.getText();
        String nickname = nicknameField.getText();

        if (phone == null || phone.isEmpty() || nickname == null || nickname.isEmpty()) {
            System.out.println("Veuillez remplir tous les champs.");
            return;
        }

        // Logique d'ajout du contact ici
        Database.addContact(user.getPhone_Number(), phone, 0, nickname);

        //Mise à jour
        allContacts.setAll(Database.getAllContacts());

        allContactsListView.setItems(allContacts);
        System.out.println("Ajout du contact: " + nickname + " (" + phone + ")");
    }


    /**
     * Sends a message to the selected contact.
     */
    @FXML
    private void sendMessage() throws SQLException {
        String messageText = messageTextField.getText(); //recuperation du message saisir


        System.out.println("Je m'apprete à envoyer le message " + messageText + "à " + selectedContact);

        if (!messageText.isEmpty() && selectedContact != null) {
            // Extraire l'ID du contact sélectionné en utilisant la méthode appropriée
            String recipientId = selectedContact.getPhone_number();


            if (recipientId != null) {
                Message message = new Message(user.getPhone_Number(), "MESSAGE", messageText, recipientId);
                message.setAck("sent");
                message.setStatut("delivered");
                //Recupération de l'address IP actuell du destinataire

                //RecipentAddress
                System.out.println("Son addresse est " + recipientAddress);
                // Avant chaque envoi (message ou fichier), assure-toi de récupérer l'adresse
                Client.requestAddress(recipientId, notification -> {
                    if ("ADDRESS_RESPONSE".equals(notification.getType())) {
                        String currentAddress = notification.getMessage();
                        selectedUserStatut.setText("online");
                        new MessageSender(currentAddress, MESSAGE_PORT, message).start();
                    } else selectedUserStatut.setText("offline");
                });

                //Ajout du message dans la zone de chat
                MessageController.addMessage(chatVBox, user.getPhone_Number(), message.getContent(), true, "read");

                //Ajout dans la BD
                Database.saveMessage(message);

                //Mise à jour du dernier message
                Database.updateContact(message);
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
            String sender = user.getPhone_Number(); //recupere l'ID du user

            if (selectedContact == null) {
                showAlert("Veuillez sélectionner un contact.");
                return;
            }
            // Extraire l'ID du contact sélectionné en utilisant la méthode appropriée
            String recipientId = selectedContact.getPhone_number();
            System.out.println(recipientId);
            sendFile(sender, recipientId); // Call the sendFile method


        } else {

            Alert alert = new Alert(Alert.AlertType.ERROR, "No user logged in", ButtonType.OK);
            alert.showAndWait();

        }
    }

    private void sendFile(String sender, String recipientId) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Send");
        File selectedFile = fileChooser.showOpenDialog(null);

        if (selectedFile != null) {
            try {
                byte[] fileData = Files.readAllBytes(selectedFile.toPath());
                String fileName = selectedFile.getName();

                //Ajout du fichier dans la zone de chat

                // Avant chaque envoi (message ou fichier), assure-toi de récupérer l'adresse
                Client.requestAddress(recipientId, notification -> {
                    if ("ADDRESS_RESPONSE".equals(notification.getType())) {
                        String currentAddress = notification.getMessage();
                        selectedUserStatut.setText("online");
                        new FileSender(currentAddress, FILE_PORT, selectedFile, sender, recipientId).start();
                    } else selectedUserStatut.setText("offline");
                });

                FileController.addFile(chatVBox, "com/alaanya/view/images/file.png", fileName, true);

                System.out.println("[CLIENT] Sending file: " + fileName + " (" + fileData.length + " bytes)");
            } catch (IOException e) {

                Alert alert = new Alert(Alert.AlertType.ERROR, "Error reading file: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();

            }
        } else {
            System.out.println("[CLIENT] File selection cancelled.");
        }
    }


    /**
     * Loads the conversation between the current user and the selected contact.
     *
     * @param currentUserId The military ID of the current user.
     * @param contactId     The military ID of the selected contact.
     */
    private void loadConversation(String currentUserId, String contactId) {

        //VIDE LA ZONED DE CHAT
        chatVBox.getChildren().clear();

        //recuperation des conversations
        List<Message> conversation = Database.getChatDetails(currentUserId, contactId);

        for (Message message : conversation) {
            String sender = message.getSender().equals(currentUserId) ? "Vous" : message.getSender();

            if (Objects.equals(message.getType(), "MESSAGE"))
                MessageController.addMessage(chatVBox, sender, message.getContent(), !Objects.equals(message.getAck(), "receive"), message.getStatut());
            if (Objects.equals(message.getType(), "file"))
                FileController.addFile(chatVBox, sender, message.getContent(), !Objects.equals(message.getAck(), "receive"));
        }
    }


    @FXML
    private void logout() {
        try {
            //fermer le port


            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/alaanya/view/LoginView.fxml"));
            GridPane loginView = (GridPane) loader.load();

            Scene scene = new Scene(loginView);
            Stage stage = MainApp.getPrimaryStage();
            stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println(e.getMessage() + " : " + e.getCause());
        }
    }


    public void sendMessageKeyPressed(KeyEvent event) throws SQLException {
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

    //filtrage des contacts
    private void filterContacts(String query) {
        if (query == null || query.isEmpty()) {
            contactListView.setItems(messageContacts);
            return;
        }

        String lower = query.toLowerCase();
        List<Contact> filtered = messageContacts.stream()
                .filter(c -> c.getName().toLowerCase().contains(lower) || c.getPhone_number().contains(query))
                .collect(Collectors.toList());
        contactListView.setItems(FXCollections.observableArrayList(filtered));
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.showAndWait();
    }


    /**
     * Recherche des utilisateurs par ID militaire ou nom d'utilisateur.
     */
    @FXML
    private void searchUsers() {
        String query = searchTextField.getText().trim(); // texte saisi

        if (query.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez saisir un nom ou un numéro à rechercher.", ButtonType.OK);
            alert.showAndWait();
            return;
        }

        try {
            // Appelle la méthode de recherche (à implémenter dans ta classe Database)
            Collection<? extends Contact> results = Database.searchUsers(query, user.getPhone_Number());


            ObservableList<Contact> observableResults = FXCollections.observableArrayList(results);
            contactListView.setItems(observableResults);


        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur lors de la recherche : " + e.getMessage(), ButtonType.OK);
            alert.showAndWait();
        }
    }


    public void openProfile(ActionEvent actionEvent) {
    }

    @FXML
    private void callUser(ActionEvent event) {
        if (recipientAddress == null || recipientAddress.isEmpty()) {
            showError("Aucun contact sélectionné", "Veuillez sélectionner un contact avant d'appeler.");
            return;
        }

        if (user == null) {
            showError("Utilisateur inconnu", "Vos informations d'utilisateur sont manquantes.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alaanya/view/audio_chat.fxml"));
            Parent root = loader.load(); // Charge le fichier FXML

            AudioChatController controller = loader.getController(); // Récupère le contrôleur initialisé
            controller.initCall(recipientAddress, user.getUsername() + " (" + user.getPhone_Number() + ")");

            Stage stage = new Stage();
            stage.setTitle("Appel audio");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de lancer la fenêtre d'appel.");
        }

    }

    public void startVideoCall(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alaanya/view/VideoChat.fxml"));
            Parent root = loader.load();

            VideoChatController controller = loader.getController();
            controller.initVideoCall(recipientAddress,user.getUsername() + " (" + user.getPhone_Number() + ")");

            Stage stage = new Stage();
            stage.setTitle("Appel Vidéo");
            stage.setScene(new Scene(root));
           // stage.initModality(Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors du chargement de la vue d'appel vidéo : " + e.getMessage());
        }
    }


    public void showOptions(ActionEvent actionEvent) {
    }

    // Méthode pour afficher la zone des paramètres
    public void showSettings() {
        settingsVBox.setVisible(true); // Afficher les paramètres
        settingsUserLabel.setText("Utilisateur: " + userLabel.getText());
        settingsGradeLabel.setText("Grade: " + gradeLabel.getText());
        settingsDivisionLabel.setText("Division: " + divisionLabel.getText());
        settingsIdLabel.setText("ID: " + idLabel.getText());
    }

    // Méthode pour fermer la zone des paramètres
    public void closeSettings() {
        settingsVBox.setVisible(false); // Masquer les paramètres
    }


    @FXML
    private void toggleAddContactPane(ActionEvent event) {
        boolean isVisible = addContactPane.isVisible();
        addContactPane.setVisible(!isVisible);
        addContactPane.setManaged(!isVisible);
    }

    // Méthode pour basculer entre les thèmes (sombre et clair)
    @FXML
    private void toggleThemeAction() {
        // Vérifiez si les ressources CSS existent dans le chemin spécifié
        URL darkThemeUrl = getClass().getResource("/com/alaanya/view/css/dark-theme.css");
        URL lightThemeUrl = getClass().getResource("/com/alaanya/view/css/light-theme.css");

        // Si les fichiers CSS ne sont pas trouvés, afficher une erreur dans la console
        if (darkThemeUrl == null || lightThemeUrl == null) {
            System.err.println("Erreur : Fichier CSS non trouvé !");
            return;  // Quitter la méthode si le fichier CSS est manquant
        }

        Stage stage = (Stage) contactsTabPane.getScene().getWindow();
        Scene scene = stage.getScene();

        boolean isDarkMode = scene.getStylesheets().contains(darkThemeUrl.toExternalForm());

        if (isDarkMode) {
            System.out.println("Dark theme");
            scene.getStylesheets().clear();
            scene.getStylesheets().add(lightThemeUrl.toExternalForm());
        } else {
            System.out.println("light theme");
            scene.getStylesheets().clear();
            scene.getStylesheets().add(darkThemeUrl.toExternalForm());
        }
    }

    private boolean showConfirmationDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Nouvel appel");
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private void openAudioChat(String ip, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/audio_chat.fxml"));
            Parent root = loader.load();
            AudioChatController controller = loader.getController();
            controller.initCall(ip, username);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Appel avec " + username);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // Méthode pour afficher une information sous forme d'alerte
    private void showInfo(String title, String message) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle(title);
        info.setContentText(message);
        info.show();
    }

}
