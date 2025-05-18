package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import video.VideoCallManager;

public class VideoChatController {

    @FXML
    public static ImageView localView;
    @FXML
    public static ImageView remoteView;
    @FXML
    public static Label callDurationLabel;
    @FXML private Button endCallButton;

    private VideoCallManager videoManager;

    public void initialize() {
        endCallButton.setOnAction(e -> {
            if (videoManager != null) {
                videoManager.hangUp();
                endCallButton.setDisable(true); // désactive après raccrochage
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
        videoManager.startSending(ip, MainController.VIDEO_PORT);
    }
}
