package controller;

import audio.AudioCallManager;
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

    @FXML private static Label callDurationLabel;
    @FXML private Button endCallButton;

    private static Timeline callTimer;
    private static int secondsElapsed = 0;

    public void initCall(String ip, String username) {
        AudioCallManager.startReceiving(MainController.AUDIO_PORT);
        AudioCallManager.startSending(ip, MainController.AUDIO_PORT);
        startCallTimer();
    }


    @FXML
    public void initialize() {
        endCallButton.setOnAction(_ -> stopCall());
    }

    public  void stopCall( ){
        stopCallTimer();
        callManager.stopAll();
        ((Stage) endCallButton.getScene().getWindow()).close();

        //envoie un signal à l'interloccuteur qu'on a raccroché
        if (MainController.signaler != null && MainController.recipientAddress != null) {
            MainController.signaler.sendCallEnd(MainController.recipientAddress, "AUDIO");
        } else {
            System.err.println("Error: signaler or recipientAddress is null.");
        }
    }

    public static void startCallTimer() {
        secondsElapsed = 0;
        if (callDurationLabel == null) {
            System.err.println("Call duration label is not initialized.");
        } else {
        callDurationLabel.setText("Durée : 00:00");
        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            int m = secondsElapsed / 60, s = secondsElapsed % 60;
            callDurationLabel.setText(String.format("Durée : %02d:%02d", m, s));
        }));
        callTimer.setCycleCount(Animation.INDEFINITE);
        callTimer.play();
        }
    }

    private void stopCallTimer() {
        if (callTimer != null) callTimer.stop();
    }
}

