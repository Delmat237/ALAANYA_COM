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
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import message.MessageReadThread;
import message.MessageWriteThread;


//import video.VideoPlayer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatApp extends Application {
    private static final double ICON_SIZE = 24; // Taille des icônes
    protected Socket socket;
    protected  Socket audioSocket;
    protected Socket fileSocket;
    private static final String HOSTNAME = "localhost";//"192.168.43.223";//
    private static final int PORT = 12348;
    private static final int PORTAUDIO = 12346;
    private static final int PORTFILE = 12347;


    public TabPane chatArea;
    private Label chatTitle;
    private TextField inputField;
    private HBox callBar;
    private Label callTimer;
    private BorderPane chatBox;
    private VBox contactList;
    private TextField contactNameField ;
    private TextField ContactId ;
    private Button chatProfile;
    private Label chatStatus;
    private  HBox messageInput;

    private Text defaultChatMessage;

    //Table mappang
    public Hashtable<String, Contact> contactChats = new Hashtable<>(); // Stocker les zones de chat par contact

    public static class Contact{
        private String IP;
        private String Name;
        private VBox chatBox;
        private boolean status; //permet de savoir si une personne est on line ou pas

        public Contact(String IP, String Name,VBox chatBox,boolean status ){
            this.IP = IP; this.Name = Name;this.chatBox = chatBox;this.status = status;}

        public Contact(String IP, String Name,VBox chatBox){
            this.IP = IP; this.Name = Name;this.chatBox = chatBox;this.status = true;}

        public VBox BoxGetter(){return this.chatBox;}
        public String GetIP(){return this.IP;}
        public String GetName(){return this.Name;}
    }
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

        new FileReceicerThread(this,fileSocket).start();



        //Barre de navigation supérieure avec le nom de l'application
        HBox topNav = new HBox();
        topNav.setPadding(new Insets(10));
        topNav.setStyle("""
                -fx-background-color: #1d6198;
                -fx-padding: 10px;
                """);
        Label appName = new Label("ALAANYA COM");
        appName.setStyle("""
                -fx-text-fill: white;
                -fx-font-size: 20px;
                """);
        topNav.getChildren().add(appName);

        // Liste de contacts sur la gauche avec des boutons cliquables

        contactList = new VBox();
        contactList.setMinWidth(200);
        contactList.setStyle("-fx-background-color: rgba(24,18,78,0.62); -fx-padding: 10px;");

        VBox serverBox = new VBox();
        serverBox.getChildren().add(new Label("I am a server"));
        contactChats.put(ServerId,new Contact(ServerId,"SERVER",new VBox()));

        //Chargement des contacts déjà enregistrés
        Enumeration<String> contacts = contactChats.keys();

//        while(contacts.hasMoreElements()){
//            String contactId = contactChats.get(contacts.nextElement()).GetIP();
//            String contactName = contactChats.get(contacts.nextElement()).GetName();
//
//            showContact(contactId,contactName);
//        }
        showContact(ServerId,"SERVER");
        Tab serverTab = new Tab(ServerId);
        serverTab.setClosable(false);
        serverTab.setId(ServerId);

        //Zone pour ajouter de nouveau contact
        VBox addContactBox = new VBox();
        contactNameField = new TextField();
        ContactId = new TextField();

        ContactId.setPromptText("Adresse IP");
        contactNameField.setPromptText("Contact name");

        Button addContactButton = new Button("+");
        addContactButton.setAlignment(Pos.CENTER_RIGHT);
        addContactButton.setStyle("""
                -fx-padding: 10px; \
                -fx-background-color: rgba(28,75,108,0.22); \
                -fx-background-radius: 5px; \
                -fx-margin-bottom: 5px;""");

        //logique du bouton d'ajout de contact
        addContactButton.setOnAction(e -> {
            String contactName = contactNameField.getText();
            String contactId= ContactId.getText();

            //ENREGISTRE LE CONATCT
            contactChats.put(contactId,new Contact(contactId,contactName,new VBox()));
            Tab contactTab = new Tab(contactId);
            contactTab.setClosable(false);
            contactTab.setId(contactId);

            chatArea.getTabs().add(contactTab);
            //AFFICHE LE CONTACT
            showContact(contactId,contactName);
            contactNameField.clear();
            ContactId.clear();
        });

        addContactBox.getChildren().addAll(contactNameField,ContactId, addContactButton);
        contactList.setSpacing(20);


        // Zone de chat où les messages seront affichés

        chatArea = new TabPane();
        chatArea.setStyle("-fx-background-color: rgba(225,218,218,0.58); -fx-padding: 10px;");
        chatArea.getTabs().add(serverTab);
        chatArea.setSide(Side.LEFT);
        chatArea.setTabMinWidth(0);
        chatArea.setTabMaxWidth(0);
        chatArea.setTabMinHeight(0);
        chatArea.setTabMaxHeight(0);

        //ScrollPane du chatArea
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(chatArea);


        scrollPane.setFitToWidth(true);

        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Conteneur pour la zone de chat avec nom du contact sélectionné

        TabPane chatFrame = new TabPane();
        chatFrame.setStyle("""
                -fx-background-color: rgba(225,218,218,0.58); \
                -fx-padding: 10px; \
                -fx-border-color: #BDBDBD;\
                 -fx-border-width: 1px;""");

        HBox chatHeader = new HBox();


        // Barre d'appel (invisible par défaut)
        callBar = new HBox(10);
        callBar.setPadding(new Insets(10));
        callBar.setStyle("-fx-background-color: #f4f4f4;");
        callBar.setAlignment(Pos.CENTER_LEFT);
        callBar.setVisible(false); // Masqué par défaut

        Label callUsernameLabel = new Label("usernameCall");
        callUsernameLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333;");

        callTimer = new Label("00:00");
        callTimer.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Button hangUpButton = createButtonWithImage("/icons/hangup.png");
        hangUpButton.setStyle("-fx-background-color: #ff4c4c;"
                + "-fx-background-radius: 50%; "
                + "-fx-min-width: 50px; "
                + "-fx-min-height: 50px; "
                + "-fx-max-width: 50px; "
                + "-fx-max-height: 50px; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 8, 0.1, 0, 2);");

        hangUpButton.setOnAction(e ->{
            callBar.setVisible(false);
            audioSendThread.interrupt();
            audioReceiveThread.interrupt();

        });

        chatProfile = createButtonWithImage("/icons/user.png");
        chatProfile.setVisible(false);
        chatProfile.setStyle("""
                        -fx-background-color: rgba(83,46,156,0.62);
                        -fx-background-radius: 50%;
                        -fx-min-width:40px;
                        -fx-min-height:40px;
                        -fx-max-width:40px;
                        -fx-max-height:40px;
                        """);

        chatProfile.setOnAction( e ->{
            Tab profileTab = new Tab();
            VBox profileBox = new VBox();
            Image image = loadImage("/icons/user.png");
            ImageView imageView = new ImageView(image);
            Text ip = new Text(contactChats.get(currentContactId).GetIP());
            ip.setTextAlignment(TextAlignment.CENTER);
            Text name = new Text(contactChats.get(currentContactId).GetName());
            name.setTextAlignment(TextAlignment.CENTER);

            profileBox.getChildren().addAll(imageView,ip, name);
            profileTab.setContent(profileBox);
            chatArea.getTabs().add(profileTab);
            chatArea.getSelectionModel().select(profileTab);
        });

        chatStatus = new Label();
        chatStatus.setStyle("-fx-background-color: green;");
        chatStatus.setAlignment(Pos.CENTER_RIGHT);

        // Espaceur dynamique pour pousser le bouton à droite
        Region spacerMainContent1 = new Region();
        HBox.setHgrow(spacerMainContent1, Priority.ALWAYS);

        Region spacerMainContent2 = new Region();
        HBox.setHgrow(spacerMainContent2, Priority.ALWAYS);

        callBar.getChildren().addAll(callUsernameLabel, spacerMainContent1, callTimer, spacerMainContent2, hangUpButton);
        callBar.setSpacing(20);

        //Boutons d'appel vidéo et audio
        Button audioCallButton = createButtonWithImage("/icons/phone-call.png");
        Button videoCallButton =createButtonWithImage("/icons/video-camera-alt.png");
        Button settingButton =createButtonWithImage("/icons/settings.png");

        // Gestion des appels audio
        audioCallButton.setOnAction(e -> {
            Platform.runLater(() -> {
                try {

                    // Threads pour les appels audio
                    audioSetup = new AudioSetup();
                    startListeningForNotifications(audioSocket); //En attende notification pour les appels audios
                    startCall(currentContactId);
                } catch (Exception ex) {
                    showError("Erreur lors de la gestion de l'appel audio : " + ex.getMessage());
                }
            });
        });

        videoCallButton.setOnAction(e -> {
            launchVideoPlayer();
        });



        //ESpaceur dynamique pour pousser les boutons à droite
        Region spacer = new Region(); //Escape séparant le nom + pp et le bouton d'appel ou setting
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Region spacer2 = new Region(); //Escape séparant le nom + pp et le bouton d'appel ou setting
        HBox.setHgrow(spacer, Priority.ALWAYS);

        //Message par default
        defaultChatMessage = new Text("Sélectionnez un contact pour commencer à discuter.");
        defaultChatMessage.setStyle("-fx-font-size: 16px;-fx-fill:#666;-fx-text-alignment: center");
        ImageView bg= new ImageView(loadImage("/icons/paper-plane.png"));

        Tab Acceuil = new Tab("Acceuil");
        VBox vBox = new VBox();
        vBox.getChildren().addAll(defaultChatMessage,bg);
        Acceuil.setContent(vBox);
        Acceuil.setClosable(false);
        chatArea.getTabs().add(Acceuil);
        chatArea.getSelectionModel().select(Acceuil);
        
        chatTitle = new Label();
        chatTitle.setStyle("-fx-fill: black;-fx-font-size: 16px;");

        chatHeader.getChildren().addAll(chatProfile,chatTitle,spacer2,chatStatus,spacer,audioCallButton,videoCallButton,settingButton);
        chatHeader.setAlignment(Pos.TOP_RIGHT);

        Tab mainTap = new Tab("Chat");
        VBox mainFrame = new VBox();
        mainFrame.getChildren().addAll(chatHeader,scrollPane);
        mainFrame.setSpacing(10);
        mainTap.setClosable(false);
        mainTap.setContent(mainFrame);

        Tab settingTab = new Tab("Settings");
        VBox settingFrame = new VBox(10);
        settingFrame.getChildren().add(settings());
        settingFrame.setSpacing(10);
        settingTab.setContent(settingFrame);
        settingTab.setClosable(false);

        chatFrame.setSide(Side.RIGHT);
        chatFrame.getTabs().addAll(mainTap,settingTab);
        chatFrame.getSelectionModel().select(mainTap);
        settingButton.setOnAction(e ->{ chatFrame.getSelectionModel().select(settingTab);});

        // Zone de saisie du message avec effet flottant

        messageInput = new HBox();
        messageInput.setStyle("""
            -fx-padding: 10px;
             -fx-background-color: rgba(225,218,218,0.58);
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
                addMessage(contactChats.get(currentContactId).BoxGetter(),currentContactId,message, true); // Message envoyé
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
                addMessage(contactChats.get(currentContactId).BoxGetter(),currentContactId,message, true); // Message envoyé

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

                try {
                    //Envoie les meta données
                    fileSend.sendMetadata(currentContactId+"&&FILE",file.getName(),file.length());
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
        messageInput.setVisible(false);
        // Combiner la zone de chat et la zone de saisie dans un BorderPane

        chatBox = new BorderPane();
        chatBox.setStyle("-fx-background-color: rgba(225,218,218,0.58);");
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

        //Lier la page css
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        //Affiche la page
        stage.show();

    }

    private void launchVideoPlayer() {
        //Créer un nouveau thread pour lancer le lecteur video
        Thread videoPlayerThread = new Thread(() -> {
            /*VideoPlayer videoPlayer = new VideoPlayer();
            videoPlayer.start(new Stage());*/
        });
        videoPlayerThread.setDaemon(true); //aSSURER QUE LE THREAD SE TERMINE QUAND L'APPLICATION SE TERMINE
        videoPlayerThread.start();
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
   InputStream stream = getClass().getResourceAsStream(path);
	if (stream == null) {
	    System.out.println("Resource not found: " + path);
	    throw new NullPointerException("Resource not found: " + path);
	    
	}
	return new Image(stream);

 
}

    private void showContact(String contactId,String contactName){
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

            HBox contact = new HBox();
            Button contactNam = new Button(contactName);
            contactNam.setStyle("""
                       -fx-padding: 10px;\s
                       -fx-background-color: rgba(28,75,108,0.22);
                       -fx-background-radius: 5px;
                      -fx-margin-bottom: 5px;""");
            contactNam.setMaxWidth(Double.MAX_VALUE);
            contactNam.setOnAction(ex -> loadChatForContact(contactId));
            contact.getChildren().addAll(contactButton,contactNam);

            contactList.getChildren().add(contact);

        }
    }

    private void loadChatForContact(String contactId) {
        currentContactId = contactId;
        for (Tab tab : chatArea.getTabs()){
            if (contactId.equals(tab.getId())){

                tab.setContent(contactChats.get(contactId).BoxGetter());
                chatArea.getSelectionModel().select(tab);
            }
        }
        chatTitle.setText(contactChats.get(currentContactId).GetName());

        chatProfile.setVisible(true);
        chatStatus.setText(contactChats.get(currentContactId).status ? "Online":"Last seen today at ");
        messageInput.setVisible(true);
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

        bottomLine.getChildren().add(timeStamp);


        VBox messageBubble = new VBox(1);

        messageBubble.setPadding(new Insets(10));
        messageBubble.setStyle(MessageFormat.format("-fx-background-color: {0};{1}", isSentByUser ? "#DCF8C6" : "#FFFFFF",
                "-fx-background-radius: 0 15 15 15; -fx-border-radius: 0 15 15 15;"));

        String senderName  = contactChats.get(senderId).GetName();
        String user = (isSentByUser ? "Vous" : senderName);
        TextFlow userName = new TextFlow(new Text(user));
        userName.setPadding(new Insets(5));
        userName.setStyle("-fx-background-color : #ECE5DD");


        Region spacer = new Region();
        spacer.minHeight(1);

        if (isSentByUser){
            userName.setTextAlignment(TextAlignment.RIGHT);
            bubbleContent.setAlignment(Pos.CENTER_RIGHT);

        }else{
            userName.setTextAlignment(TextAlignment.LEFT);
            bubbleContent.setAlignment(Pos.CENTER_LEFT);
        }
        messageBubble.getChildren().addAll(bubbleContent, spacer, bottomLine);
        messageBox.getChildren().addAll(userName, messageBubble);

        chatBox.getChildren().add(messageBox);
        
        Platform.runLater(() ->contactChats.put(senderId,new Contact(senderId,senderName,chatBox))); //ajoute dans le contactChats

        if (senderId.equals(currentContactId)){
            Platform.runLater(() -> loadChatForContact(senderId)); //Charge automatiquement a l'ecran
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


        Image fileImage = loadImage("/icons/document-signed.png");
        Button fileLabel = createButtonWithImage("/icons/document-signed.png");
        Label filename = new Label(fileName);
        fileLabel.setStyle("-fx-font-weight: bold;");

       if (isSentByUser) {
            //Button downloadButton = getDownloadButton(fileName, new File(fileName));

            //messageBox.getChildren().addAll(userName, fileBubble, downloadButton);
            messageBox.getChildren().addAll(userName, fileBubble);
            fileInfoLine.setAlignment(Pos.CENTER_RIGHT);
           userName.setTextAlignment(TextAlignment.RIGHT);
        } else {
            messageBox.getChildren().addAll(userName, fileBubble);
           userName.setTextAlignment(TextAlignment.LEFT);
           fileInfoLine.setAlignment(Pos.CENTER_LEFT);
        }

        // Ajout d'un effet de survol pour indiquer que c'est cliquable
        fileBubble.setOnMouseEntered(e
                -> fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#c5e1b0" : "#f5f5f5") + ";"
                + "-fx-background-radius: 15; -fx-border-radius: 15;"));

        fileBubble.setOnMouseExited(e
                -> fileBubble.setStyle("-fx-background-color: " + (isSentByUser ? "#DCF8C6" : "#FFFFFF") + ";"
                + "-fx-background-radius: 15; -fx-border-radius: 15;"));

        // Ajout d'un curseur pointer pour indiquer que c'est cliquable
        fileBubble.setStyle(fileBubble.getStyle() + "-fx-cursor: hand;");


        //Enregistre le message
        Platform.runLater(() -> chatBox.getChildren().add(messageBox));
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
            alert.setTitle("Incoming Call");
            alert.setHeaderText("Call from " + callerId);
            alert.setContentText("Do you want to accept this call?");

            // Bouton Accept avec une icône
            Image acceptImage = new Image("file:icons/accept.png");
            ImageView acceptIcon = new ImageView(acceptImage);
            acceptIcon.setFitWidth(16); // Taille de l'icône
            acceptIcon.setFitHeight(16);
            ButtonType acceptButtonType = new ButtonType("Accept", ButtonBar.ButtonData.OK_DONE);
            alert.getDialogPane().setGraphic(acceptIcon);//lookupButton(acceptButtonType);

            // Bouton Deny avec une icône
            Image denyImage = new Image("file:icons/deny.png"); // Remplacez par le chemin de l'icône
            ImageView denyIcon = new ImageView(denyImage);
            denyIcon.setFitWidth(16); // Taille de l'icône
            denyIcon.setFitHeight(16);
            ButtonType denyButtonType = new ButtonType("Deny", ButtonBar.ButtonData.CANCEL_CLOSE);
            //alert.getDialogPane().lookupButton(denyButtonType).setGraphic(denyIcon);

            // Ajoutez les boutons à l'alerte
            alert.getButtonTypes().setAll(acceptButtonType, denyButtonType);

            // Gérer la réponse de l'utilisateur
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == acceptButtonType) {
                startAudioCall(callerId);
            } else {
                declineAudioCall(callerId);
            }
        });
    }
    private void declineAudioCall(String callerId) {
        showError(callerId + "is busy at the moment");
    }

    private void startAudioCall(String callerId) {

        callBar.setVisible(true); //
        chatBox.setTop(callBar);
        LocalTime currentTime = LocalTime.now();
        callTimer.setText(String.valueOf(Duration.between(currentTime, LocalTime.now())));

        System.out.println("Call accepted from: " + callerId);
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

    public TabPane settings(){
        TabPane tabPane = new TabPane();

        //Créer un onglet profil
        Tab profilTab = new Tab("Profil");
        VBox profilContent = createProfileTab();
        profilTab.setContent(profilContent);

        //Créer un onglet pour nitification
        Tab notificationsTab = new Tab("Notifications");
        VBox notificationsContent = createNotificationsTab();
        notificationsTab.setContent(notificationsContent);

        //Créer un onglet pour confidentialité
        Tab privacyTab = new Tab("Confidatialité");
        VBox privacyContent = createPrivacyTab();
        privacyTab.setContent(privacyContent);

        //Ajouter les onglets au TabPane
        tabPane.getTabs().addAll(profilTab,notificationsTab,privacyTab);

       return tabPane;
    }

    private VBox createPrivacyTab() {
        VBox privacyBox = new VBox();
        CheckBox lastSeenVisibity = new CheckBox("Afficher l'heure e la dernière connexion");
        CheckBox readReceipts = new CheckBox("Receevoir des accusés de reception");

        Button saveButton =  new Button("Enregistrer");

        privacyBox.getChildren().addAll(lastSeenVisibity,readReceipts,saveButton);
        return privacyBox;
    }

    private VBox createNotificationsTab() {
        VBox notificationBox = new VBox(10);
        CheckBox messageNotifications = new CheckBox("Rcevoir des notifications de message");
        CheckBox groupNotifications = new CheckBox("Recevoir des notifications de groupe");

        Slider soundVolumeSlider = new Slider(0,100,50);
        soundVolumeSlider.setShowTickLabels(true);
        soundVolumeSlider.setBlockIncrement(10);

        Button saveButton = new Button("Enregister");

        notificationBox.getChildren().addAll(messageNotifications, groupNotifications,new Label("Volume Sonore") ,soundVolumeSlider, saveButton);
        return notificationBox;
    }

    private VBox createProfileTab() {
        VBox profilBox = new VBox(10);
        Label nameLabel = new Label("Nom");
        TextField nameField = new TextField("A.L.D");
        Label statusLabel = new Label("Status");
        TextField statusField = new TextField("Je suis occupé");
        Button updateProfileButton = new Button("Mise à jour");
        profilBox.getChildren().addAll(nameLabel,nameField,statusLabel,statusField,updateProfileButton);
        return profilBox;
    }



    public static void main(String[] args) {

        launch(args);
    }
}
