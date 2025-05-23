package controller;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.alaanya.MainApp;

import audio.AudioCallManager;
import audio.AudioSetup;
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
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox; // Ensure this import is correct and the class exists in the specified package
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import message.MessageReceiver;
import message.MessageSender;
import model.Contact;
import model.Message;
import model.User;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import signal.CallSignaler;
import socket.Client;
import utils.SoundPlayer;
import utils.ThemeManager;
import video.VideoCallManager;

@SuppressWarnings({"CallToPrintStackTrace","unused","FieldMayBeFinal","exports"})

public class MainController {


    @FXML public VBox downloadsVBox;
    @FXML private Button toggleThemeButton;
    @FXML private ScrollPane scrollPane;
    @FXML private TabPane mainTabPane;
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

    private FontIcon lightIcon = new FontIcon("fas-sun");
    private FontIcon darkIcon = new FontIcon("fas-moon");


    private User user;
    public static String recipientAddress;

    private ObservableList<Contact> messageContacts = FXCollections.observableArrayList();
    private ObservableList<Contact> allContacts = FXCollections.observableArrayList();
    public static Contact selectedContact;
    private final AudioSetup audioSetup = new AudioSetup();

    static int FILE_PORT = 5001;
    static int MESSAGE_PORT = 5000;
    public static final int AUDIO_PORT = 5002;
    public static final int SIGNAL_PORT = 5003;
    public static int VIDEO_PORT = 5004;

    private  MessageReceiver receiver;
    private FileReceiver fileReceiver;

    private boolean isCallDialogOpen = false;

    //Signaler pour gerer la signalisation lors des appels
    public static CallSignaler signaler;
    private int seconds = 0;
    private Timeline callTimer;

    private static final Logger logger = Logger.getLogger(MainController.class.getName());
    @FXML
    public void initialize() throws SQLException {

        String soundStart = "/sounds/start.wav";
        SoundPlayer.playSound(soundStart);

        //Thread de reception des messages texte
        receiver = new MessageReceiver(MESSAGE_PORT);
        receiver.setMessageListener(this::handleIncomingMessage);
        receiver.start();

        //Thread de reception des fichiers
        fileReceiver = new FileReceiver(FILE_PORT);
        fileReceiver.setFileListener(this::handleIncomingMessage);
        fileReceiver.start();

        //Initialization pour l'attente des appels
        signaler = CallSignaler.getInstance(); // Singleton instance
        signaler.listenForCallRequests(new CallSignaler.CallListener() {
            @Override
            public void onCallReceived(String fromUser, String ip, String type) {
                if (isCallDialogOpen) return;
                isCallDialogOpen = true;
                if (selectedContact == null)

                Platform.runLater(() -> {
                    boolean accepted = showConfirmationDialog("Appel " + type + " de " + fromUser);
                    SoundPlayer.stopSound();
                    signaler.sendCallResponse(ip, accepted, type);
                    if (accepted) {
                        if ("AUDIO".equals(type)) {
                            openAudioChat(ip, fromUser);
                            AudioCallManager.startSending(ip, AUDIO_PORT);
                        } else if ("VIDEO".equals(type)) {
                            openVideoChat(ip, fromUser);
                            VideoCallManager.startSending(ip,VIDEO_PORT);
                        }
                    }
                });
            }

            @Override
            public void onCallAccepted(String ip, String type) {
                Platform.runLater(() -> {
                    if ("AUDIO".equals(type)) {
                        AudioCallManager.startSending(ip, AUDIO_PORT);
                        openAudioChat(ip, selectedContact.getName());
                        
                        
                    } else if ("VIDEO".equals(type)) {
                        openVideoChat(ip,  selectedContact.getName());
                        VideoCallManager.startSending(ip,VIDEO_PORT);

                    }
                });
            }

            //ecoute si l'interloccuteur a raccroché
            @Override
            public void onCallEnd(String ip, String type){
                Platform.runLater(() -> {
                    CallSignaler.stopInstance();
                   //controller.stopCall(ip,type);
                });
            }

            @Override
            public void onCallDeclined(String ip) {
                Platform.runLater(() ->{
                        showInfo("Appel refusé", "L’utilisateur a refusé l’appel.");
                        CallSignaler.stopInstance();
                        }
                );
            }
        });

        // Filtrage en mémoire pour "Mes contacts"
        searchTextField.textProperty().addListener((obs, oldValue, newValue) -> filterContacts(newValue));

        //Personnalisation de la liste des contacts
        ContactListView(contactListView);
        ContactListView(allContactsListView);

        scrollPane.setVvalue(1.0);


        defaultCenterVBox.setVisible(true);
        showChatArea(false);


        //mettre le theme par defaut

        Platform.runLater(() -> {
            Scene scene = contactsTabPane.getScene();
            if (scene != null) {
                ThemeManager.applyCurrentTheme(scene); // Par défaut : LIGHT
            }
        });

    }

    public void ContactListView(ListView<Contact> listView) {
        //Personnalisation de l'affichage des contacts
        listView.setCellFactory(list -> new ListCell<>() {
            private final HBox content;
            private final ImageView imageView;
            private final Label name;
            private final Label lastMessage;
            private final Label date;
            private final Label unreadCount;

            {
                imageView = new ImageView();
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

                VBox texts = new VBox(name, lastMessage);
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

        //Action qui s'execute lorsqu'on clique sur un contact
        listView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {

                    if (newValue != null) {
                        selectedContact = newValue;
                        setSelectedUsername(newValue.getName());
                        showChatArea(true);

                        String userId = newValue.getPhone_number();
                        //charge les conversations
                        loadConversation(user.getPhone_Number(), userId);

                        Client.requestAddress(userId, notification -> Platform.runLater(() -> {
                            if ("ADDRESS_RESPONSE".equals(notification.getType())) {
                                recipientAddress = notification.getMessage();
                                selectedUserStatut.setText("online");
                            } else if ("ADDRESS_NOT_FOUND".equals(notification.getType())) {

                                Alert alert = new Alert(Alert.AlertType.ERROR, " Address not found for user: " + newValue, ButtonType.OK);
                                alert.showAndWait();

                                selectedUserStatut.setText("offline");
                            }
                        }));
                    } else {
                        showChatArea(false);
                    }
                });

    }

    private void handleIncomingMessage(Message message) {
        Platform.runLater(() -> {
            // Envoyer une confirmation de lecture si le message recu n'est pas deja une confirmation de lecture
            if (!Objects.equals(message.getType(), "ACK_READ")) { //si le message n'est pas un accusé de reception
                //affiche une notification si le contact couraant n'est pas le recepteur
                if(Objects.equals(message.getRecipient(), selectedContact.getPhone_number()))

                    showInfo("Nouveau message de "+message.getSender(),message.getContent());
                //Mise à jour du contact en enregistrant le lastmessage
                try {
                    Database.updateContact(message);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                //Si le message recu est un message
                if (Objects.equals(message.getType(), "MESSAGE"))
                    //ajouter dans la zone de chat
                    MessageController.addMessage(chatVBox, message.getSender(), message.getContent(), false, "read",message.getTimestamp());
                else
                    FileController.addFile(chatVBox, message.getSender(), message.getFileName(), false);

                //envoyer une confirmation de lecture
                Message readAck = new Message(user.getPhone_Number(), "ACK_READ", "read", message.getSender());
                new MessageSender(recipientAddress, MESSAGE_PORT, readAck).start();

            }
            // Mettre à jour du status du message  dans la base de données
            try {
                Database.updateMessageStatus(message, "read");
            } catch (SQLException e) {
                throw new RuntimeException(e);
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

    public User getUser() {return user;}

    /**
     * Ajoute un contact à la liste de contacts de l'utilisateur.
     */
    @FXML
    private void addContact() throws SQLException {
        String phone = phoneNumberField.getText();
        String nickname = nicknameField.getText();

        if (phone == null || phone.isEmpty() || nickname == null || nickname.isEmpty()) {
            logger.info("Veuillez remplir tous les champs.");
            return;
        }

        // Logique d'ajout du contact ici
        Database.addContact(user.getPhone_Number(), phone, 0, nickname);

        //Mise à jour
        allContacts.setAll(Database.getAllContacts());

        allContactsListView.setItems(allContacts);
        logger.info("Ajout du contact: " + nickname + " (" + phone + ")");
    }


    /**
     * Sends a message to the selected contact.
     */
    @FXML
    private void sendMessage() throws SQLException {
        SoundPlayer.playSound("sounds/pop.mp3");
        String messageText = messageTextField.getText(); //recuperation du message saisir
        logger.info("Je m'apprete à envoyer le message " + messageText + "à " + selectedContact);

        if (!messageText.isEmpty() && selectedContact != null) {
            // Extraire l'ID du contact sélectionné en utilisant la méthode appropriée
            String recipientId = selectedContact.getPhone_number();
            if (recipientId != null) {
                Message message = new Message(user.getPhone_Number(), "MESSAGE", messageText, recipientId);
                message.setAck("sent");
                message.setStatut("delivered");

                //RecipentAddress
                logger.info("Son addresse est " + recipientAddress);
                // Avant chaque envoi (message ou fichier), assure-toi de récupérer l'adresse
                Client.requestAddress(recipientId, notification -> {
                    if ("ADDRESS_RESPONSE".equals(notification.getType())) {
                        String currentAddress = notification.getMessage();
                        selectedUserStatut.setText("online");
                        new MessageSender(currentAddress, MESSAGE_PORT, message).start();
                        //Ajout du message dans la zone de chat
                        MessageController.addMessage(chatVBox, user.getPhone_Number(), message.getContent(), true, "read",message.getTimestamp());
                    } else {
                        selectedUserStatut.setText("offline");
                        //Ajout du message dans la zone de chat en marquant non envoyé
                        MessageController.addMessage(chatVBox, user.getPhone_Number(), message.getContent(), true, "delivered",message.getTimestamp());
                    }
                });

                //Ajout dans la BD
                Database.saveMessage(message);

                //Mise à jour du dernier message
                Database.updateContact(message);
                messageTextField.clear();
            } else {
                logger.info("Erreur : Impossible d'extraire l'ID du contact.");

            }
        } else {
            logger.info("Veuillez sélectionner un contact et saisir un message.");
        }
    }

    // Method to select a file and send it
    @FXML
    private void sendFile() {
        if (user != null) {
            String sender = user.getPhone_Number(); //recupere l'ID du user

            if (selectedContact == null) {
                showAlert();
                return;
            }
            // Extraire l'ID du contact sélectionné en utilisant la méthode appropriée
            String recipientId = selectedContact.getPhone_number();
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

                // Avant chaque envoi (message ou fichier), assure-toi de récupérer l'adresse
                Client.requestAddress(recipientId, notification -> {
                    if ("ADDRESS_RESPONSE".equals(notification.getType())) {
                        String currentAddress = notification.getMessage();
                        selectedUserStatut.setText("online");
                        new FileSender(currentAddress, FILE_PORT, selectedFile, sender, recipientId).start();
                    } else selectedUserStatut.setText("offline");
                });

                //Ajout du fichier dans la zone de chat
                FileController.addFile(chatVBox, selectedFile.getPath(), fileName, true);

                logger.info("[CLIENT] Sending file: " + fileName + " (" + fileData.length + " bytes)");
            } catch (IOException e) {

                Alert alert = new Alert(Alert.AlertType.ERROR, "Error reading file: " + e.getMessage(), ButtonType.OK);
                alert.showAndWait();

            }
        } else {
            logger.info("[CLIENT] File selection cancelled.");
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
                MessageController.addMessage(chatVBox, sender, message.getContent(), !Objects.equals(message.getAck(), "receive"), message.getStatut(), message.getTimestamp());

            if (Objects.equals(message.getType(), "FILE"))
                FileController.addFile(chatVBox, message.getFilePath(), message.getFileName(), !Objects.equals(message.getAck(), "receive"));
        }
    }


    @FXML
    private void logout() {
        try {
            //fermer les ports

            receiver.interrupt();
            fileReceiver.interrupt();
            FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/alaanya/view/LoginView.fxml"));
            StackPane loginView = loader.load();

            Scene scene = new Scene(loginView);
            Stage stage;
            stage = (Stage) userLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            logger.warning(e.getMessage() + " : " + e.getCause());
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
            logger.info("Recherche d'un contact");
            searchUsers();
        }
    }

    @FXML
    public void searchContacts(ActionEvent event) {
        logger.info("Recherche d'un contact");
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

        CallSignaler signaler = CallSignaler.getInstance();
        signaler.sendCallRequest(recipientAddress, user.getUsername() + " (" + user.getPhone_Number() + ")","AUDIO");
    }


    public void startVideoCall(ActionEvent actionEvent) {
        if (recipientAddress == null || recipientAddress.isEmpty()) {
            showError("Aucun contact sélectionné", "Veuillez sélectionner un contact avant d'appeler.");
            return;
        }

        if (user == null) {
            showError("Utilisateur inconnu", "Vos informations d'utilisateur sont manquantes.");
            return;
        }

        CallSignaler videoCallSignaler = CallSignaler.getInstance();
        videoCallSignaler.sendCallRequest(recipientAddress, user.getUsername() + " (" + user.getPhone_Number() + ")","VIDEO");
    }



    public void showOptions(ActionEvent actionEvent) {
    }

    // Méthode pour afficher la zone des paramètres
    @FXML
    public void showSettings() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(1); // Onglet "Paramètres"
        }

        if (settingsVBox != null) {
            settingsVBox.setVisible(true); // Affiche le VBox (juste au cas où)
        }

        settingsUserLabel.setText("Utilisateur: " + userLabel.getText());
        settingsGradeLabel.setText("Grade: " + gradeLabel.getText());
        settingsDivisionLabel.setText("Division: " + divisionLabel.getText());
        settingsIdLabel.setText("ID: " + idLabel.getText());
    }

    @FXML
    public void closeSettings() {
        if (mainTabPane != null) {
            mainTabPane.getSelectionModel().select(0); // Retour à l'onglet "Chat"
        }
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
        Scene scene = toggleThemeButton.getScene();
        if (scene != null) {
            ThemeManager.toggleTheme(scene);

            if (ThemeManager.getCurrentTheme() == ThemeManager.Theme.DARK) {
                toggleThemeButton.setGraphic(lightIcon);
            } else {
                toggleThemeButton.setGraphic(darkIcon);
            }
        }
    }


    private void openAudioChat(String ip, String username) {
        logger.info("Appel accepté");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alaanya/view/audio_chat.fxml"));
            Parent root = loader.load();

            // Récupération du contrôleur et initialisation
            AudioChatController controller = loader.getController();
            controller.initCall(ip, username);

            // Création de la fenêtre
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Appel avec " + username);

            // Fermeture manuelle : on libère l'appel audio + CallSignaler
            stage.setOnCloseRequest(event -> {
                logger.info("❌ Fermeture de la fenêtre d'appel audio.");
                if (controller != null) {
                    controller.cleanup(); // 💡 À implémenter dans AudioChatController
                }
                CallSignaler.stopInstance();
            });

            stage.show();

        } catch (IOException e) {
            logger.severe("Erreur ouverture appel audio : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void openVideoChat(String ip, String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/alaanya/view/VideoChat.fxml"));
            Parent root = loader.load();

            VideoChatController videoChatController = loader.getController();
            videoChatController.setup(ip, username);

            Stage stage = new Stage();
            stage.setTitle("Appel Vidéo");
            stage.setScene(new Scene(root));
            stage.setResizable(true);


            // 🔁 Ajout de la gestion fermeture de la fenêtre
            stage.setOnCloseRequest(event -> {
                System.out.println("❌ Fenêtre fermée manuellement.");
                if (videoChatController != null) {
                    videoChatController.cleanup(); // Appel d’une méthode à créer
                }
                CallSignaler.stopInstance(); // Libération de l’instance
            });

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            logger.warning("Erreur lors du chargement de la vue d'appel vidéo : " + e.getMessage());
        }
    }


    private boolean showConfirmationDialog(String message) {
        // Sonnerie
        SoundPlayer.playSound("/sounds/urgent.wav");

        // Création de l'alerte
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("📞 Nouvel appel");
        alert.setHeaderText("Voulez-vous accepter l'appel ?");
        alert.setContentText(message);

        // Création des boutons personnalisés avec icônes FontIcon
        ButtonType acceptBtnType = new ButtonType("Accepté", ButtonBar.ButtonData.YES);
        ButtonType declineBtnType = new ButtonType("Refusé", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(acceptBtnType, declineBtnType);

        // Ajout d'icônes après lookup
        DialogPane pane = alert.getDialogPane();

        // Icône verte pour Accepté
        Button acceptBtn = (Button) pane.lookupButton(acceptBtnType);
        FontIcon acceptIcon = new FontIcon(FontAwesomeSolid.CHECK_CIRCLE);
        acceptIcon.setIconSize(18);
        acceptIcon.setIconColor(Color.GREEN);
        acceptBtn.setGraphic(acceptIcon);
        acceptBtn.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");

        // Icône rouge pour Refusé
        Button declineBtn = (Button) pane.lookupButton(declineBtnType);
        FontIcon declineIcon = new FontIcon(FontAwesomeSolid.TIMES_CIRCLE);
        declineIcon.setIconSize(18);
        declineIcon.setIconColor(Color.RED);
        declineBtn.setGraphic(declineIcon);
        declineBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

        // Affichage de l'alerte
        Optional<ButtonType> result = alert.showAndWait();
        return result.orElse(declineBtnType) == acceptBtnType;
    }

    private void showError(String title, String message) {
        String soundError = "/sounds/tone.wav";
        SoundPlayer.playSound(soundError);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    // Méthode pour afficher une information sous forme d'alerte
    private void showInfo(String title, String message) {
        String soundNotification = "/sounds/not.wav";
        SoundPlayer.playSound(soundNotification);
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle(title);
        info.setContentText(message);
        info.show();
    }

    private void showAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR, "Veuillez sélectionner un contact.", ButtonType.OK);
        alert.showAndWait();
    }

}
