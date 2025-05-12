package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import signal.CallSignaler;
import video.CameraService;
import video.VideoReceiver;
import video.VideoSender;

public class VideoChatController {

    @FXML private ImageView localView;
    @FXML private ImageView remoteView;
    @FXML private Label callDurationLabel;
    @FXML private Button endCallButton;

    private final CallSignaler signaler = new CallSignaler();

    private VideoSender sender;
    private VideoReceiver receiver;
    private CameraService camera;

    private String lastCallerIp;
    private Timeline callTimer;
    private int secondsElapsed = 0;
    private String remoteIP ;
    private String username;

    public void initVideoCall(String recipientAddress, String s) {
        this.remoteIP = recipientAddress;
        this.username = username;
        initialize();
    }
    // Initialisation de la fenêtre d'appel
    public void initialize() {
        // Rendre le label de la durée d'appel visible dès le début
        callDurationLabel.setVisible(false);

        // Configuration du bouton de fin d'appel
        endCallButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        endCallButton.setOnAction(e -> hangUp());

        // Envoie un signalement d'appel
        startCall();

        signaler.listenForCallRequests(new CallSignaler.CallListener() {
            @Override
            public void onCallReceived(String fromUser, String ip) {
                lastCallerIp = ip;
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Appel entrant");
                    alert.setHeaderText("Appel de " + fromUser);
                    alert.setContentText("Accepter l'appel ?");
                    alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            signaler.sendCallResponse(ip, true);
                            startReceive();
                            startCall(ip);
                            startCallTimer();
                        } else {
                            signaler.sendCallResponse(ip, false);
                        }
                    });
                });
            }

            @Override
            public void onCallAccepted(String ip) {
                Platform.runLater(() -> {
                    startReceive();
                    startCall(ip);
                    startCallTimer();
                    showInfo("Appel accepté", "L'autre utilisateur a accepté l'appel.");
                });
            }

            @Override
            public void onCallDeclined(String ip) {
                Platform.runLater(() -> showInfo("Appel refusé", "L'appel a été refusé par " + ip));
            }
        });
    }

    // Méthode pour démarrer un appel vidéo
    @FXML
    public void startCall() {

        signaler.sendCallRequest(remoteIP, username);
        lastCallerIp = remoteIP;
    }

    // Méthode pour démarrer un appel vidéo à une IP spécifique
    public void startCall(String ip) {
        camera = new CameraService(frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            localView.setImage(fxImage); // Afficher la vidéo locale
        });
        camera.start();

        sender = new VideoSender(ip, 6000, camera);
        sender.start();
    }

    // Méthode pour démarrer la réception du flux vidéo
    public void startReceive() {
        receiver = new VideoReceiver(6001, frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            remoteView.setImage(fxImage); // Afficher la vidéo distante
        });
        receiver.start();
    }

    // Méthode pour démarrer le chronomètre de l'appel
    private void startCallTimer() {
        secondsElapsed = 0;
        callDurationLabel.setText("Durée : 00:00");
        callDurationLabel.setVisible(true);

        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            secondsElapsed++;
            int minutes = secondsElapsed / 60;
            int seconds = secondsElapsed % 60;
            callDurationLabel.setText(String.format("Durée : %02d:%02d", minutes, seconds));
        }));
        callTimer.setCycleCount(Timeline.INDEFINITE);
        callTimer.play();
    }

    // Méthode pour arrêter le chronomètre
    private void stopCallTimer() {
        if (callTimer != null) {
            callTimer.stop();
            callTimer = null;
        }
        secondsElapsed = 0;
        callDurationLabel.setText("Durée : 00:00");
        callDurationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
    }

    // Méthode pour raccrocher l'appel
    @FXML
    public void hangUp() {
        if (sender != null) {
            sender.stopSending();
            sender = null;
        }

        if (receiver != null) {
            receiver.stopReceiving();
            receiver = null;
        }

        if (camera != null) {
            camera.stopCapture();
            camera = null;
        }

        stopCallTimer();

        showInfo("Appel terminé", "Vous avez raccroché.");
    }

    // Méthode pour afficher une information sous forme d'alerte
    private void showInfo(String title, String message) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle(title);
        info.setContentText(message);
        info.show();
    }


}
