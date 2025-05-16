package video;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.animation.Timeline;
import signal.CallSignaler;

public class VideoCallManager {

    private final CallSignaler signaler = CallSignaler.getInstance();
    private static VideoSender sender;
    private static VideoReceiver receiver;
    private static CameraService camera;
    private Timeline callTimer;

    private static ImageView localView = null;
    private static ImageView remoteView = null;
    private final Runnable onHangUp;
    private final Label callDurationLabel;

    public VideoCallManager(ImageView localView, ImageView remoteView,
                            Label callDurationLabel,
                            Runnable onCallAccepted, Runnable onHangUp) {
        VideoCallManager.localView = localView;
        VideoCallManager.remoteView = remoteView;
        this.callDurationLabel = callDurationLabel;
        this.onHangUp = onHangUp;
    }

    public void startOutgoingCall(String ip, String username) {
        signaler.sendCallRequest(ip, username,"VIDEO");
    }

    public static void startReceiving(int port){
        receiver = new VideoReceiver(port, frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            remoteView.setImage(fxImage);
        });
        receiver.start();
    }
    public static void startSending(String ip, int port) {
        camera = new CameraService(frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            Platform.runLater(() -> localView.setImage(fxImage));
        });
        camera.start();

        sender = new VideoSender(ip, port, camera);
        sender.start();
    }

    private void stopCallTimer() {
        if (callTimer != null) {
            callTimer.stop();
            callTimer = null;
        }
        int secondsElapsed = 0;
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

}
