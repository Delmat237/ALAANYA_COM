package controller;

import audio.AudioReceiver;
import audio.AudioSender;
import audio.AudioSetup;
import audio.CallSignaler;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AudioChatController {
    private final AudioSetup audioSetup = new AudioSetup();
    private final CallSignaler signaler = new CallSignaler();

    private AudioSender sender;
    private AudioReceiver receiver;
    private String lastCallerIp;


    private Label callDurationLabel;

    private Timeline callTimer;
    private int secondsElapsed = 0;


    public void initialize() {
        // Créer une nouvelle fenêtre (popup)
        Stage callWindow = new Stage();
        callWindow.initModality(Modality.APPLICATION_MODAL);
        callWindow.setTitle("Appel en cours");

        // Initialiser le label de durée d'appel
        callDurationLabel = new Label("Durée : 00:00");
        callDurationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        callDurationLabel.setVisible(false);

        // Bouton raccrocher
        Button endCallButton = new Button("Raccrocher");
        endCallButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");
        endCallButton.setOnAction(e -> {
            if (callTimer != null) {
                callTimer.stop();
                hangUp();
            }
            callWindow.close();
        });

        // Layout
        VBox layout = new VBox(15, callDurationLabel, endCallButton);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        // Affichage
        Scene scene = new Scene(layout, 250, 150);
        callWindow.setScene(scene);
        callWindow.show();

        // Envoie un signalement d'appel
        startCall();

        // Écoute des appels entrants
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


    @FXML
    public void startCall() {
        System.out.println("Je lance la requete");
        String remoteIP = "localhost";  // ou récupéré dynamiquement
        String username = "Delmat";
        signaler.sendCallRequest(remoteIP, username);
        lastCallerIp = remoteIP;
    }

    public void startCall(String ip) {
        sender = new AudioSender(ip, 5000, audioSetup);
        sender.start();
    }

    public void startReceive() {
        receiver = new AudioReceiver(5000, audioSetup);
        receiver.start();
    }

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

    private void stopCallTimer() {
        if (callTimer != null) {
            callTimer.stop();
            callTimer = null;
        }
        secondsElapsed = 0;
        callDurationLabel.setText("Durée : 00:00");
        callDurationLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

    }



    public void hangUp() {
        if (sender != null) {
            sender.stopSending();
            sender = null;
        }

        if (receiver != null) {
            receiver.stopReceiving();
            receiver = null;
        }

        audioSetup.closeMicrophone();
        audioSetup.closeSpeakers();
        stopCallTimer();

        showInfo("Appel terminé", "Vous avez raccroché.");
    }

    private void showInfo(String title, String message) {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle(title);
        info.setContentText(message);
        info.show();
    }
}
