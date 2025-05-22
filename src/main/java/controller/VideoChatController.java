package controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import video.VideoCallManager;

public class VideoChatController {

    @FXML private  ImageView localView;
    @FXML private  ImageView remoteView;
    @FXML private  Label callDurationLabel;
    @FXML private Button endCallButton;

    private VideoCallManager videoManager;

    private  Timeline callTimer;
    private  int secondsElapsed = 0;


    public void initialize() {
        endCallButton.setOnAction(e -> {
            if (videoManager != null) {
                videoManager.hangUp();
                endCallButton.setDisable(true); // désactive après raccrochage
                stopCallTimer();
                 ((Stage) endCallButton.getScene().getWindow()).close();

            }
        });
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

        videoManager.startReceiving(MainController.VIDEO_PORT);
        if (!VideoCallManager.startSending(ip, MainController.VIDEO_PORT)) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("Caméra indisponible");
                alert.setContentText("Votre caméra n'a pas pu démarrer.");
                alert.showAndWait();
            });
        }

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
}
