package video;

import controller.VideoChatController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import signal.CallSignaler;

import static controller.VideoChatController.*;

public class VideoCallManager {

    private final CallSignaler signaler = CallSignaler.getInstance();

    private static VideoSender sender;
    private   VideoReceiver receiver;
    private static CameraService camera;
    private  Timeline callTimer;


    private final Runnable onHangUp;

    private  int secondsElapsed = 0;

    public VideoCallManager(ImageView localView, ImageView remoteView,
                            Label callDurationLabel,
                            Runnable onCallAccepted, Runnable onHangUp) {
        VideoChatController.localView  = localView;
        VideoChatController.remoteView = remoteView;
        VideoChatController.callDurationLabel = callDurationLabel;
        this.onHangUp = onHangUp;
    }

    public  void startReceiving(int port) {
        try {
            receiver = new VideoReceiver(port, frame -> {
                Image fxImage = SwingFXUtils.toFXImage(frame, null);
                Platform.runLater(() -> remoteView.setImage(fxImage));
            });
            receiver.start();
        } catch (Exception e) {
            System.err.println("❌ Impossible de démarrer le récepteur vidéo :");

        }
    }

    public  void startSending(String ip, int port) {
        camera = new CameraService(frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            Platform.runLater(() -> localView.setImage(fxImage));
        });

        try {
            camera.start();
            sender = new VideoSender(ip, port, camera);
            sender.start();
            startCallTimer();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage de l’envoi vidéo :");

        }
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

    public void hangUp() {
        System.out.println("📞 Fin de l’appel...");

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

        if (onHangUp != null) {
            Platform.runLater(onHangUp);
        }
    }
}
