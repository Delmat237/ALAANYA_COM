package alaanya;

import audio.AudioReceiveThread;
import audio.AudioSendThread;
import audio.AudioSetup;
import file.FileReceicerThread;
import file.FileSendThread;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import message.MessageReadThread;
import message.MessageWriteThread;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ChatApp extends Application {
    private static final double ICON_SIZE = 24; // Taille des icônes
    protected Socket socket;
    protected  Socket audioSocket;
    protected Socket fileSocket;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 12345;
    private static final int PORTAUDIO = 12346;
    private static final int PORTFILE = 12347;


    public VBox chatArea;
    private Label chatTitle;
    private TextField inputField;

    private final Map<String, VBox> contactChats = new HashMap<>(); // Stocker les zones de chat par contact
    private String ServerId;
    public  String currentContactId = ServerId; // Identifiant du contact actuellement sélectionné
    public String senderId;

    // Gestion des threads audio
    private AudioSetup audioSetup;
    private AudioSendThread audioSendThread;
    private AudioReceiveThread audioReceiveThread;
    private String message;

    @Override
    public void start(Stage stage) throws IOException {

        try{
            this.socket = new Socket(HOSTNAME,PORT);
            this.audioSocket = new Socket(HOSTNAME,PORTAUDIO);
            this.fileSocket = new Socket(HOSTNAME,PORTFILE);
            //ServerId = socket.getInetAddress().getHostAddress();
            ServerId = "127.0.1.1";

        }catch(IOException e)
        {
            showError("Impossible de se connecter au serveur ! "+e.getMessage());
        }

        //THread pour  les messages texte
        MessageWriteThread messageSend = new MessageWriteThread(socket);
        messageSend.start();

        new MessageReadThread(socket, this).start();

        //Thread pour les fichiers
        FileSendThread fileSend = new FileSendThread(this,fileSocket);
        fileSend.start();

        new FileReceicerThread(this,fileSocket);

        // Threads pour les appels audio
        audioSetup = new AudioSetup();
        startListeningForNotifications(audioSocket); //En attende notification pour les appels audios


        //Barre de navigation supérieure avec le nom de l'application
        HBox topNav = new HBox();
        topNav.setPadding(new Insets(10));
        topNav.setStyle("""
                -fx-background-color: lightblue;
                -fx-padding: 10px;
                """);
        Label appName = new Label("ALAANYA COM");
        appName.setStyle("""
                -fx-text-fill: white;
                -fx-font-size: 20px;
                """);
        topNav.getChildren().add(appName);

        // Liste de contacts sur la gauche avec des boutons cliquables

        VBox contactList = new VBox();
        contactList.setStyle("-fx-background-color: #ECE5DD; -fx-padding: 10px;");

        for (int i = 0; i <= contactChats.size(); i++) {
            Button contactButton = new Button("SERVER");
            contactButton.setStyle("""
                       -fx-padding: 10px;\s
                       -fx-background-color: rgba(28,75,108,0.22);
                       -fx-background-radius: 5px;
                      -fx-margin-bottom: 5px;""");
            contactButton.setMaxWidth(Double.MAX_VALUE);

            contactButton.setOnAction(e -> loadChatForContact(ServerId));
            contactList.getChildren().add(contactButton);
        }

        VBox addContactBox = new VBox();
        TextField contactNameField = new TextField();
        TextField ContactId = new TextField();

        ContactId.setPromptText("Adresse IP");
        contactNameField.setPromptText("Contact name");

        Button addContactButton = new Button("+");
        addContactButton.setStyle("""
                -fx-padding: 10px; \
                -fx-background-color: rgba(181,181,188,0.63); \
                -fx-background-radius: 5px; \
                -fx-margin-bottom: 5px;""");

        addContactButton.setOnAction(e -> {
            String contactName = contactNameField.getText();
            String contactId= ContactId.getText();

            if (!contactName.isEmpty() && !contactId.isEmpty()) {

                Button contactButton = createButtonWithImage("/icons/user.png");
                contactButton.setStyle("""
                       
                        -fx-background-color: #B9BCC6;
                        -fx-background-radius: 50%;
                        -fx-min-width:40px;
                        -fx-min-height:40px;
                        -fx-max-width:40px;
                        -fx-max-height:40px;
                        """);
                contactButton.setMaxWidth(Double.MAX_VALUE);


                contactButton.setOnAction(ex -> loadChatForContact(contactId));
                contactList.getChildren().add(contactButton);
                contactNameField.clear();
                ContactId.clear();
            }
        });

        addContactBox.getChildren().addAll(contactNameField,ContactId, addContactButton);
        contactList.setSpacing(50);


        // Zone de chat où les messages seront affichés

        chatArea = new VBox();
        chatArea.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10px;");

        // Conteneur pour la zone de chat avec nom du contact sélectionné

        VBox chatFrame = new VBox();
        chatFrame.setStyle("""
                -fx-background-color: #F0F0F0; \
                -fx-padding: 10px; \
                -fx-border-color: #BDBDBD;\
                 -fx-border-width: 1px;""");

        HBox chatHeader = new HBox();

        chatTitle = new Label("Chat");
        chatTitle.setStyle("-fx-fill: black;-fx-font-size: 16px;");

        Button chatProfile = createButtonWithImage("/icons/user.png");
        chatProfile.setStyle("""
                        -fx-background-color: #B9BCC6;
                        -fx-background-radius: 50%;
                        -fx-min-width:40px;
                        -fx-min-height:40px;
                        -fx-max-width:40px;
                        -fx-max-height:40px;
                        """);

        //Boutons d'appel vidéo et audio
        Button audioCallButton = createButtonWithImage("/icons/phone-call.png");
        Button settingButton =createButtonWithImage("/icons/video-camera-alt.png");

        // Gestion des appels audio
        audioCallButton.setOnAction(e -> {
            Platform.runLater(() -> {
                try {
                    startCall(currentContactId);
                } catch (Exception ex) {
                    showError("Erreur lors de la gestion de l'appel audio : " + ex.getMessage());
                }
            });
        });

        //ESpaceur dynamique pour pousser les boutons à droite
        Region spacer = new Region(); //Espace séparant le nom + pp et le bouton d'appel ou setting
        HBox.setHgrow(spacer, Priority.ALWAYS);

        chatHeader.getChildren().addAll(chatProfile,chatTitle,spacer,audioCallButton,settingButton);
        chatHeader.setAlignment(Pos.TOP_RIGHT);
        chatFrame.getChildren().addAll(chatHeader,chatArea);
        chatFrame.setSpacing(10);

        // Zone de saisie du message avec effet flottant

        HBox messageInput = new HBox();
        messageInput.setStyle("""
            -fx-padding: 10px;
             -fx-background-color: rgb(255,255,255);
            \s""");

        HBox messageInputLayout = new  HBox();
        messageInputLayout.setPadding(new Insets(0, 5, 0, 0));
        messageInputLayout.setAlignment(Pos.CENTER);
        messageInputLayout.setStyle("""
            -fx-padding: 10px;
            -fx-background-color: ECE5DD;
            -fx-border-radius: 25px; /* Rayon pour la bordure */
            -fx-background-radius: 25px; /* Rayon pour l'arrière-plan */
            -fx-border-color: rgba(225,218,218,0.58); /* Couleur de la bordure */
            -fx-border-width: 2px;  /* Épaisseur de la bordure */
        """);

        // TextField stylisé (InputField)
        inputField = new TextField();
        inputField.setPromptText("Type a message...");
        inputField.setStyle("""
            -fx-background-color: rgba(255,255,255,0.46);\s
            -fx-text-fill: #333333;\s
            -fx-font-size: 14px;\s
            -fx-border-color: transparent;\s
            -fx-background-radius: 30;\s
            -fx-border-radius: 30;\s
            -fx-padding: 10;\s
            -fx-effect: dropshadow(gaussian, rgba(225,218,218,0.58), 8, 0.1, 0, 2);
       \s""");
        HBox.setHgrow(inputField,Priority.ALWAYS);

        //Boutons d'envoi des messages
        Button sendButton = createButtonWithImage("/icons/paper-plane.png");
        sendButton.setStyle("""
                -fx-background-color: rgba(13,94,197,0.63); \
                -fx-background-radius: 50%; \
                -fx-min-width: 50px; \
                -fx-min-height: 50px; \
                -fx-max-width: 50px; \
                -fx-max-height: 50px; \
                -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.1, 0, 2);""");


        //Boutons d'envoies des fichiers
        Button sendFileButton = createButtonWithImage("/icons/clip.png");

        // Espacement automatique pour un alignement parfait
        inputField.setPrefWidth(980);
        sendButton.setPrefWidth(60);
        sendButton.setPrefHeight(60);
        sendFileButton.setPrefWidth(50);

        //bOUTONS POUR LE VOICE
        Button voiceButton = createButtonWithImage("/icons/microphone.png");

        ProgressBar progressBar =  new ProgressBar(0);
        progressBar.setPrefWidth(100);//taille de la barre

        //Actions des boutons
        sendButton.setOnAction(e -> {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                try {
                    messageSend.send(currentContactId+"&&"+message);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                //Ajoute le message dans le chatArea et le contactChats
                addMessage(getChatBox(currentContactId),currentContactId,message, true); // Message envoyé
                inputField.clear();
            }
        });

        inputField.setOnAction(_ -> {
            String message = inputField.getText();
            if (!message.isEmpty()) {
                try {
                    messageSend.send(currentContactId+"&&"+message);
                } catch (IOException ex) {
                    showError("Impossible d'envoyer le message :"+ex.getMessage());
                }
                //Ajoute le message dans le chatArea et le contactChats
                addMessage(getChatBox(currentContactId),currentContactId,message, true); // Message envoyé
                inputField.clear();
            }
        });

        sendFileButton.setOnAction(e ->{
            FileChooser fileChooser = new FileChooser();
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*.*"));
            fileChooser.setTitle("Choisir un fichier");
            File file = fileChooser.showOpenDialog(stage);

            if (file != null){
                String  filePath = file.getAbsolutePath();
                //Envoie les meta données
                try {

                    messageSend.send(currentContactId+"&&FILE"); // Indique un fichier en cours d'envoi
                    messageSend.send(file.getName()); // Envoie le nom du fichier
                    messageSend.sendLong(file.length()); // Envoie la taille totale du fichier

                    fileSend.send(file,progressBar);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                }else{
                    showError("No file selected");
                }

        });

        messageInputLayout.getChildren().addAll(inputField,sendFileButton);
        messageInput.getChildren().addAll(messageInputLayout, voiceButton,sendButton);//,progressBar);

        messageInput.setSpacing(20);

        // Combiner la zone de chat et la zone de saisie dans un BorderPane

        BorderPane chatBox = new BorderPane();
        chatBox.setCenter(chatFrame);
        chatBox.setBottom(messageInput);
        BorderPane.setMargin(messageInput, new Insets(10));

        BorderPane contactBox = new BorderPane();
        contactBox.setCenter(contactList);
        contactBox.setBottom(addContactBox);

        // Layout principal
        HBox mainLayout = new HBox();
        mainLayout.getChildren().addAll(contactBox, chatBox);
        HBox.setHgrow(chatBox, Priority.ALWAYS);

        // Layout racine
        BorderPane rootLayout = new BorderPane();
        rootLayout.setTop(topNav);
        rootLayout.setCenter(mainLayout);

        // Créer une scène
        Scene scene = new Scene(rootLayout, 800, 600);
        stage.setScene(scene);
        stage.setTitle("ALAANYA COM");
        stage.show();

    }


    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }


    private Button createButtonWithImage(String iconPath) {
        Button button = new Button();
        ImageView icon = new ImageView(loadImage(iconPath));
        icon.setFitWidth(ICON_SIZE);
        icon.setFitHeight(ICON_SIZE);
        button.setGraphic(icon);
        button.setStyle("-fx-background-color: transparent;");
        return button;
    }

    // Charger une image depuis les ressources
    private Image loadImage(String path) {
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }


    private void loadChatForContact(String contactId) {
        currentContactId = contactId;

        VBox chatBox = getChatBox(contactId); // Récupère ou crée la boîte de chat pour le contact

        Platform.runLater(() -> {
            chatArea.getChildren().clear(); // Efface uniquement la zone d'affichage actuelle
            chatArea.getChildren().addAll(chatBox.getChildren()); // Charge les messages du contact
        });

        chatTitle.setText("Chat avec Contact " + contactId);
    }

    // Ajouter un message dans la boîte de messages
    public void addMessage(VBox chatBox,String senderId,String message, boolean isSentByUser) {

        VBox messageBox = new VBox(2);

        // Création d'un HBox pour contenir le message et l'heure
        HBox bubbleContent = new HBox(1);

        HBox bottomLine = new HBox(1);
        bottomLine.setAlignment(Pos.CENTER_RIGHT);

        Text messageText = new Text(message);

        // Wrapping pour les longs messages uniquement
        double maxWidth = 200; // Limite de largeur maximale en pixels
        if (messageText.getBoundsInLocal().getWidth() > maxWidth) {
            messageText.setWrappingWidth(maxWidth); // Active le retour à la ligne si le texte dépasse la largeur
        }
        Text timeStamp = new Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeStamp.setStyle("-fx-fill: #666666; -fx-font-size: 10px;");

        bubbleContent.getChildren().addAll(messageText);
        if (isSentByUser){
            bubbleContent.setAlignment(Pos.CENTER_RIGHT);
        }else{
            bubbleContent.setAlignment(Pos.CENTER_LEFT);
        }


        bottomLine.getChildren().add(timeStamp);

        VBox messageBubble = new VBox(1);
        messageBubble.setPadding(new Insets(10));
        messageBubble.setStyle(MessageFormat.format("-fx-background-color: {0};{1}", isSentByUser ? "#DCF8C6" : "#FFFFFF",
                isSentByUser ? "-fx-border-radius: 15 0 15 15;" : "-fx-background-radius: 0 15 15 15; -fx-border-radius: 0 15 15 15;"));

        String user = (isSentByUser ? "Vous" : senderId);
        TextFlow userName = new TextFlow(new Text(user));
        userName.setPadding(new Insets(5));
        userName.setStyle("-fx-background-color : #ECE5DD");

        Region spacer = new Region();
        spacer.minHeight(1);

        messageBubble.getChildren().addAll(bubbleContent, spacer, bottomLine);
        messageBox.getChildren().addAll(userName, messageBubble);


        alignMessageContent(isSentByUser, messageBox);

        //Enregistre le message
        Platform.runLater(() ->chatBox.getChildren().add(messageBox)); //Ajoute dans le chatBox correspondant
        Platform.runLater(() ->contactChats.put(currentContactId, chatBox)); //ajoute dans le contactChats
        Platform.runLater(() -> loadChatForContact(currentContactId)); //Charge automatiquement a l'ecran

    }

    private void alignMessageContent(boolean isSentByUser, VBox messageBox) {
        if (isSentByUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }
    }

    // Ajouter un fichier dans la boîte de messages

    public void addFile(VBox chatBox,String iconPath, String fileName, boolean isSentByUser) {


        VBox messageBox = new VBox(2);

        // Conteneur principal de la bulle
        VBox fileBubble = new VBox(5);
        fileBubble.setPadding(new Insets(8));
        fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#26631b" : "#FFFFFF") + ";"
                + "-fx-background-radius: 15; -fx-border-radius: 15;");

        // Ligne supérieure avec icône et nom du fichier
        HBox fileInfoLine = new HBox(10);

        ImageView fileIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream(iconPath))));
        fileIcon.setFitWidth(35);
        fileIcon.setFitHeight(35);
        fileIcon.setPreserveRatio(true);

        // Zone de texte avec nom du fichier
        VBox fileDetails = new VBox(2);
        Text fileNameText = new Text(fileName);

        // Wrapping pour les longs messages uniquement
        double maxWidth = 250; // Limite de largeur maximale en pixels
        if (fileNameText.getBoundsInLocal().getWidth() > maxWidth) {
            fileNameText.setWrappingWidth(maxWidth); // Active le retour à la ligne si le texte dépasse la largeur
        }
        fileNameText.setStyle("-fx-font-size: 14px;");

        // Taille du fichier (exemple)
        Text fileSize = new Text("Document");
        fileSize.setStyle("-fx-fill: #667781; -fx-font-size: 12px;");

        fileDetails.getChildren().addAll(fileNameText, fileSize);
        fileInfoLine.getChildren().addAll(fileIcon, fileDetails);

        // Ligne inférieure avec l'heure
        HBox bottomLine = new HBox();
        bottomLine.setAlignment(Pos.CENTER_RIGHT);
        Text timeStamp = new Text(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeStamp.setStyle("-fx-fill: #667781; -fx-font-size: 11px;");
        bottomLine.getChildren().add(timeStamp);

        // Séparateur
        Region spacer = new Region();
        spacer.setMinHeight(5);

        fileBubble.getChildren().addAll(fileInfoLine, spacer, bottomLine);

        String user = (isSentByUser ? "Vous" : "...");
        TextFlow userName = new TextFlow(new Text(user));
        userName.setPadding(new Insets(5));
        userName.setStyle("-fx-background-color : #ECE5DD");

        messageBox.getChildren().addAll(userName, fileBubble);

        // Ajout d'un effet de survol pour indiquer que c'est cliquable
        fileBubble.setOnMouseEntered(e
                -> fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#c5e1b0" : "#f5f5f5") + ";"
                + "-fx-background-radius: 15; -fx-border-radius: 15;"));

        fileBubble.setOnMouseExited(e
                -> fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#DCF8C6" : "#FFFFFF") + ";"
                + "-fx-background-radius: 15; -fx-border-radius: 15;"));

        // Ajout d'un curseur pointer pour indiquer que c'est cliquable
        fileBubble.setStyle(fileBubble.getStyle() + "-fx-cursor: hand;");

        // Gestion de l'alignement
        alignMessageContent(isSentByUser, messageBox);

        //Enregistre le message
        Platform.runLater(() -> chatBox.getChildren().add(messageBox));
    }

    private void addFileToChat(VBox chatBox, String fileName, File file, boolean isSender, Stage stage) {
        VBox fileBox = new VBox();
        fileBox.setAlignment(isSender ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        fileBox.setSpacing(10);

        Image fileImage = loadImage("/icons/document-signed.png");
        Button fileLabel = createButtonWithImage("/icons/document-signed.png");
        Label filename = new Label(fileName);
        fileLabel.setStyle("-fx-font-weight: bold;");

        if (!isSender) {
            Button downloadButton = getDownloadButton(fileName, file, stage);

            fileBox.getChildren().addAll(fileLabel,filename, downloadButton);
        } else {
            fileBox.getChildren().addAll(fileLabel,filename);
        }

        // Ajoute le conteneur de fichier au chatBox
        Platform.runLater(() -> {
            if (!chatBox.getChildren().contains(fileBox)) {
                chatBox.getChildren().add(fileBox);
            }
        });
    }

    private static Button getDownloadButton(String fileName, File file, Stage stage) {
        Button downloadButton = new Button("Télécharger");
        downloadButton.setOnAction(e -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setInitialFileName(fileName);
                File saveLocation = fileChooser.showSaveDialog(stage); // Utilise le Stage principal

                if (saveLocation != null) {
                    Files.copy(file.toPath(), saveLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Fichier téléchargé : " + saveLocation.getAbsolutePath());
                }
            } catch (IOException ex) {
                System.out.println("Erreur lors du téléchargement : " + ex.getMessage());
            }
        });
        return downloadButton;
    }

    //Methode pour recuperer ou creer la boite de chat d'un contact
    public VBox getChatBox(String contactId) {
        // Vérifie si une boîte de chat existe déjà pour le contact
        if (!contactChats.containsKey(contactId)) {
            VBox chatBox = new VBox();
            chatBox.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 10px;");
            contactChats.put(contactId, chatBox);
        }
        return contactChats.get(contactId);
    }

    //Thread pour écouter les notifications du serveur Audio
    public void startListeningForNotifications(Socket socket) {
        new Thread(() -> {
            try (InputStream in = socket.getInputStream()){
                byte[] buffer = new byte[1024];
                while(true) {
                    int bytesRead = in.read(buffer);
                    if (bytesRead > 0) {
                        String message = new String(buffer, 0, bytesRead);
                        if (message.startsWith("CALL_INCOMING")) {
                            String callerId = message.split(":")[1];
                            handleIncomingCall(callerId);
                        }
                    }
                }
            }catch (IOException ex) {
                System.err.println("Erreur lors de la réception des notifications : " + ex.getMessage());
            }
        }).start();
    }

    private void handleIncomingCall(String callerId) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Incoming call");
            alert.setHeaderText("Call of "+callerId);
            alert.setContentText("Do you want to accept this call?");

            ButtonType acceptButtonType = new ButtonType("Accept");
            ButtonType denyButtonType = new ButtonType("Deny");
            alert.getButtonTypes().setAll(acceptButtonType, denyButtonType);

            Optional<ButtonType> result = alert.showAndWait();
            if(result.isPresent()  && result.get() == acceptButtonType) {
                startAudioCall(callerId);
            }else declineAudioCall(callerId);
        });
    }

    private void declineAudioCall(String callerId) {
        showError(callerId + "is busy at the moment");
    }

    private void startAudioCall(String callerId) {

        if (audioSendThread == null || !audioSendThread.isAlive()) {
            // Démarrer les threads audio
            audioSendThread = new AudioSendThread(audioSocket, audioSetup.microphone);
            audioReceiveThread = new AudioReceiveThread(audioSocket, audioSetup.speakers);

            audioSendThread.start();
            audioReceiveThread.start();

            showInfo("Appel audio", "Appel audio démarré avec succès.");
        } else {
            // Arrêter les threads audio
            audioSendThread.interrupt();
            audioReceiveThread.interrupt();

            showInfo("Appel audio", "Appel audio terminé.");
        }
    }

    public void startCall(String receiverId) throws IOException {
        try {
            OutputStream out = audioSocket.getOutputStream();
            String callRequest = "START_CALL:" + receiverId;
            out.write(callRequest.getBytes());
            out.flush();
        }catch (IOException ex) {showError("Erreur lors de l'initiation de l'appel : "+ex.getMessage());}
    }

    public static void main(String[] args) {

        launch(args);
    }
}
