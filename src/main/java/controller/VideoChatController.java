package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import video.VideoCallManager;

public class VideoChatController {

    @FXML private ImageView localView;
    @FXML private ImageView remoteView;
    @FXML private Label callDurationLabel;
    @FXML private Button endCallButton;

    private VideoCallManager videoManager;

    public void initialize() {
        endCallButton.setOnAction(e -> {
            if (videoManager != null) {
                videoManager.hangUp();
            }
        });
    }

    public void setup(String ip, String username) {
        videoManager = new VideoCallManager(
                localView,
                remoteView,
                callDurationLabel,
                () -> System.out.println("Appel accepté."),
                () -> {
                    System.out.println("Appel terminé.");
                    endCallButton.setDisable(true);
                }
        );
        videoManager.startOutgoingCall(ip, username);
    }


}
