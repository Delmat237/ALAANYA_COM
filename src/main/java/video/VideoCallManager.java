package video;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.util.Duration;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import signal.CallSignaler;

public class VideoCallManager {

    private final CallSignaler signaler = new CallSignaler();
    private VideoSender sender;
    private VideoReceiver receiver;
    private CameraService camera;
    private Timeline callTimer;
    private int secondsElapsed = 0;

    private final ImageView localView;
    private final ImageView remoteView;
    private final Runnable onHangUp;
    private final Runnable onCallAccepted;
    private final javafx.scene.control.Label callDurationLabel;

    public VideoCallManager(ImageView localView, ImageView remoteView,
                            javafx.scene.control.Label callDurationLabel,
                            Runnable onCallAccepted, Runnable onHangUp) {
        this.localView = localView;
        this.remoteView = remoteView;
        this.callDurationLabel = callDurationLabel;
        this.onCallAccepted = onCallAccepted;
        this.onHangUp = onHangUp;
    }

    public CallSignaler getSignaler() {
        return signaler;
    }

    public void startOutgoingCall(String ip, String username) {
        signaler.sendCallRequest(ip, username);
    }

    public void acceptCall(String ip) {
        signaler.sendCallResponse(ip, true);
        startReceiving();
        startSending(ip);
        startCallTimer();
    }

    public void rejectCall(String ip) {
        signaler.sendCallResponse(ip, false);
    }

    public void startSending(String ip) {
        camera = new CameraService(frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            Platform.runLater(() -> localView.setImage(fxImage));
        });
        camera.start();

        sender = new VideoSender(ip, 6000, camera);
        sender.start();
    }

    public void startReceiving() {
        receiver = new VideoReceiver(6001, frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            Platform.runLater(() -> remoteView.setImage(fxImage));
        });
        receiver.start();
    }

    private void startCallTimer() {
        secondsElapsed = 0;
        callDurationLabel.setText("Durée : 00:00");
        callDurationLabel.setVisible(true);

        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsElapsed++;
            int min = secondsElapsed / 60;
            int sec = secondsElapsed % 60;
            callDurationLabel.setText(String.format("Durée : %02d:%02d", min, sec));
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
        if (camera != null) {
            camera.stopCapture();
            camera = null;
        }

        stopCallTimer();
        Platform.runLater(onHangUp);
    }

    public void listenForCalls() {
        signaler.listenForCallRequests(new CallSignaler.CallListener() {
            @Override
            public void onCallReceived(String fromUser, String ip) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Appel Vidéo");
                    alert.setHeaderText("Appel entrant de " + fromUser);
                    alert.setContentText("Souhaitez-vous accepter ?");
                    alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            acceptCall(ip);
                            if (onCallAccepted != null) Platform.runLater(onCallAccepted);
                        } else {
                            rejectCall(ip);
                        }
                    });
                });
            }

            @Override
            public void onCallAccepted(String ip) {
                Platform.runLater(() -> {
                    startReceiving();
                    startSending(ip);
                    startCallTimer();
                });
            }

            @Override
            public void onCallDeclined(String ip) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Appel refusé");
                    alert.setContentText("L'appel a été refusé.");
                    alert.show();
                });
            }
        });
    }
}
