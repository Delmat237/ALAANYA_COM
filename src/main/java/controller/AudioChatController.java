package controller;

import audio.*;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class AudioChatController {
    private final AudioCallManager callManager = new AudioCallManager();

    @FXML private Label callDurationLabel;
    @FXML private Button endCallButton;

    private Timeline callTimer;
    private int secondsElapsed = 0;

    public void initCall(String ip, String username) {
        callManager.startReceiving(MainController.AUDIO_PORT);
        callManager.startSending(ip, MainController.AUDIO_PORT);
        startCallTimer();
    }


    @FXML
    public void initialize() {
        endCallButton.setOnAction(e -> {
            stopCallTimer();
            callManager.stopAll();
            ((Stage) endCallButton.getScene().getWindow()).close();
        });
    }

    private void startCallTimer() {
        secondsElapsed = 0;
        callDurationLabel.setText("Durée : 00:00");
        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            int m = secondsElapsed / 60, s = secondsElapsed % 60;
            callDurationLabel.setText(String.format("Durée : %02d:%02d", m, s));
        }));
        callTimer.setCycleCount(Animation.INDEFINITE);
        callTimer.play();
    }

    private void stopCallTimer() {
        if (callTimer != null) callTimer.stop();
    }
}

