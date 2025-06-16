package controller;

import audio.AudioCallManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

public class AudioChatController {
    private final AudioCallManager callManager = new AudioCallManager();
    
    @FXML private Label callerLabel;
    @FXML private Label callDurationLabel;
    @FXML private Button endCallButton;
    @FXML private Button muteButton;
    @FXML private Button speakerButton;
    @FXML private Button keypadButton;
    @FXML private Button addCallButton;

    private Timeline callTimer;
    private int secondsElapsed = 0;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;

    public void initCall(String ip, String username) {
        try {
            AudioCallManager.startReceiving(MainController.AUDIO_PORT);
            AudioCallManager.startSending(ip, MainController.AUDIO_PORT);
            startCallTimer();
            callerLabel.setText("Appel avec " + username);
            
            // Initialiser les tooltips
            muteButton.setTooltip(new Tooltip("Muet"));
            speakerButton.setTooltip(new Tooltip("Haut-parleur"));
            endCallButton.setTooltip(new Tooltip("Raccrocher"));
            keypadButton.setTooltip(new Tooltip("Clavier"));
            addCallButton.setTooltip(new Tooltip("Ajouter un appel"));
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de l'appel: " + e.getMessage());
            stopCall();
        }
    }

    @FXML
    public void initialize() {
        endCallButton.setOnAction(_ -> stopCall());
        muteButton.setOnAction(_ -> toggleMute());
        speakerButton.setOnAction(_ -> toggleSpeaker());
        
        // Désactiver les boutons non implémentés (optionnel)
        keypadButton.setDisable(true);
        addCallButton.setDisable(true);
    }

    private void toggleMute() {
        isMuted = !isMuted;
        callManager.setMute(isMuted);
        FontIcon icon = (FontIcon) muteButton.getGraphic();
        icon.setIconLiteral(isMuted ? "fas-microphone-slash" : "fas-microphone");
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        callManager.setSpeakerMode(isSpeakerOn);
        FontIcon icon = (FontIcon) speakerButton.getGraphic();
        icon.setIconLiteral(isSpeakerOn ? "fas-volume-mute" : "fas-volume-up");
    }

    public void stopCall() {
        stopCallTimer();
        callManager.stopAll();
        
        // Envoyer un signal de fin d'appel
        if (MainController.signaler != null && MainController.recipientAddress != null) {
            MainController.signaler.sendCallEnd(MainController.recipientAddress, "AUDIO");
        } else {
            System.err.println("Error: signaler or recipientAddress is null.");
        }
        
        // Fermer la fenêtre
        Stage stage = (Stage) endCallButton.getScene().getWindow();
        stage.close();
    }

    public void startCallTimer() {
        secondsElapsed = 0;
        callDurationLabel.setText("Appel en cours - 00:00");
        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            int minutes = secondsElapsed / 60;
            int seconds = secondsElapsed % 60;
            callDurationLabel.setText(String.format("Appel en cours - %02d:%02d", minutes, seconds));
        }));
        callTimer.setCycleCount(Animation.INDEFINITE);
        callTimer.play();
    }

    private void stopCallTimer() {
        if (callTimer != null) {
            callTimer.stop();
        }
    }

    public void cleanup() {
        stopCallTimer();
        if (callManager != null) {
            callManager.stopAll();
        }
    }
}