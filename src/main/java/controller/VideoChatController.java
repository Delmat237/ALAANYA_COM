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

public class VideoChatController {

    @FXML private Label callerLabel;
    @FXML private  ImageView localView;
    @FXML private  ImageView remoteView;
    @FXML private  Label callDurationLabel;
    @FXML private Button endCallButton;

    private VideoCallManager videoManager;

    private  Timeline callTimer;
    private  int secondsElapsed = 0;


    public void initialize() {
        endCallButton.setOnAction(e -> stopCall());
    }

    public  void stopCall( ){
        if (videoManager != null) {
            videoManager.hangUp();
            endCallButton.setDisable(true); // désactive après raccrochage
            stopCallTimer();
            ((Stage) endCallButton.getScene().getWindow()).close();
        }
        stopCallTimer();
        ((Stage) endCallButton.getScene().getWindow()).close();

        //envoie un signal à l'interloccuteur qu'on a raccroché
        if (MainController.signaler != null && MainController.recipientAddress != null) {
            MainController.signaler.sendCallEnd(MainController.recipientAddress, "VIDEO");
        } else {
            System.err.println("Error: signaler or recipientAddress is null.");
        }
    }

    public void setup(String ip, String username) {
        videoManager = new VideoCallManager(
                localView,
                remoteView,
                callDurationLabel,
                () -> System.out.println("📞 Appel vidéo accepté."),
                () -> {
                    System.out.println("❌ Appel vidéo terminé.");
                    endCallButton.setDisable(true);
                }
        );
        callerLabel.setText(username);

        videoManager.startReceiving(MainController.VIDEO_PORT);
        VideoCallManager.startSending(ip, MainController.VIDEO_PORT);

        startCallTimer();
    }

      private  void startCallTimer() {
        secondsElapsed = 0;
        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            int minutes = secondsElapsed / 60;
            int seconds = secondsElapsed % 60;
            if (callDurationLabel != null) {
                Platform.runLater(() ->
                        callDurationLabel.setText(String.format("Durée : %02d:%02d", minutes, seconds)));
            } else {
                System.out.println("⚠️ callDurationLabel est null !");
            }
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

        if (callDurationLabel != null) {
            Platform.runLater(() -> callDurationLabel.setText("Durée : 00:00"));
        } else {
            System.out.println("⚠️ callDurationLabel est null !");
        }
    }
    public void cleanup() {
        if (videoManager != null) {
            videoManager.hangUp();
        }
    }

}
