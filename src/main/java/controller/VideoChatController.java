package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import video.VideoCallManager;
import audio.AudioCallManager;
import org.kordamp.ikonli.javafx.FontIcon;

public class VideoChatController {

    @FXML private Label callerLabel;
    @FXML private ImageView localView;
    @FXML private ImageView remoteView;
    @FXML private Label callDurationLabel;
    @FXML private Button endCallButton;
    @FXML private Button muteButton;
    @FXML private Button speakerButton;
    @FXML private Button videoToggleButton;

    private VideoCallManager videoManager;
    private Timeline callTimer;
    private int secondsElapsed = 0;
    private boolean isMuted = false;
    private boolean isSpeakerOn = false;
    private boolean isVideoOn = true;

    public void initialize() {
        setupButtonActions();
        setupButtonIcons();
    }

    private void setupButtonActions() {
        endCallButton.setOnAction(e -> stopCall());
        muteButton.setOnAction(e -> toggleMute());
        speakerButton.setOnAction(e -> toggleSpeaker());
        videoToggleButton.setOnAction(e -> toggleVideo());
    }

    private void setupButtonIcons() {
        muteButton.setGraphic(new FontIcon("fas-microphone"));
        speakerButton.setGraphic(new FontIcon("fas-volume-up"));
        videoToggleButton.setGraphic(new FontIcon("fas-video"));
        endCallButton.setGraphic(new FontIcon("fas-phone-slash"));
    }

    public void stopCall() {
        stopCallTimer();
        
        if (videoManager != null) {
            videoManager.hangUp();
        }
        
        AudioCallManager.stopAll();
        
        // Notify remote peer
        if (MainController.signaler != null && MainController.recipientAddress != null) {
            MainController.signaler.sendCallEnd(MainController.recipientAddress, "VIDEO");
        } else {
            System.err.println("Error: signaler or recipientAddress is null.");
        }
        
        // Close window
        Stage stage = (Stage) endCallButton.getScene().getWindow();
        stage.close();
    }

    public void setup(String ip, String username) {
            VideoCallManager.initialize(
                localView, 
                remoteView, 
                callDurationLabel,
                () -> System.out.println("Appel accepté"),
                () -> System.out.println("Appel terminé")
            );
            
        callerLabel.setText("Appel avec " + username);
        
        // Start video streams
        VideoCallManager.startReceiving(MainController.VIDEO_PORT);
        VideoCallManager.startSending(ip, MainController.VIDEO_PORT);
        
        // Start audio streams
        AudioCallManager.startReceiving(MainController.AUDIO_PORT);
        AudioCallManager.startSending(ip, MainController.AUDIO_PORT);
        
        startCallTimer();
    }

    private void startCallTimer() {
        secondsElapsed = 0;
        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            int minutes = secondsElapsed / 60;
            int seconds = secondsElapsed % 60;
            Platform.runLater(() -> 
                callDurationLabel.setText(String.format("Appel en cours - %02d:%02d", minutes, seconds))
            );
        }));
        callTimer.setCycleCount(Timeline.INDEFINITE);
        callTimer.play();
    }

    private void stopCallTimer() {
        if (callTimer != null) {
            callTimer.stop();
            callTimer = null;
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        AudioCallManager.setMute(isMuted);
        FontIcon icon = (FontIcon) muteButton.getGraphic();
        icon.setIconLiteral(isMuted ? "fas-microphone-slash" : "fas-microphone");
    }

    private void toggleSpeaker() {
        isSpeakerOn = !isSpeakerOn;
        AudioCallManager.setSpeakerMode(isSpeakerOn);
        FontIcon icon = (FontIcon) speakerButton.getGraphic();
        icon.setIconLiteral(isSpeakerOn ? "fas-volume-mute" : "fas-volume-up");
    }

    private void toggleVideo() {
        isVideoOn = !isVideoOn;
        if (videoManager != null) {
            videoManager.toggleVideo(isVideoOn);
        }
        FontIcon icon = (FontIcon) videoToggleButton.getGraphic();
        icon.setIconLiteral(isVideoOn ? "fas-video" : "fas-video-slash");
    }

    public void cleanup() {
        stopCallTimer();
        if (videoManager != null) {
            videoManager.hangUp();
        }
        AudioCallManager.stopAll();
    }
}