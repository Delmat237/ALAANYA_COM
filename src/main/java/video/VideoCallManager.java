package video;

import controller.VideoChatController;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import signal.CallSignaler;

import static controller.VideoChatController.*;
import controller.MainController;

public class VideoCallManager {

    private final CallSignaler signaler = CallSignaler.getInstance();

    private static VideoSender sender;
    private   VideoReceiver receiver;
    private static CameraService camera;
    private static ImageView localView;
    private static ImageView remoteView;
    private static Label callDurationLabel;



    private final Runnable onHangUp;



    public VideoCallManager(ImageView localView, ImageView remoteView,
                            Label callDurationLabel,
                            Runnable onCallAccepted, Runnable onHangUp) {
                                VideoCallManager.localView = localView;
                                VideoCallManager.remoteView = remoteView;
                                VideoCallManager.callDurationLabel = callDurationLabel;
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

    public static  void startSending(String ip, int port) {
        camera = new CameraService(frame -> {
            Image fxImage = SwingFXUtils.toFXImage(frame, null);
            Platform.runLater(() -> localView.setImage(fxImage));
        });

        try {
            camera.start();
            sender = new VideoSender(ip, port, camera);
            sender.start();
           
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage de l’envoi vidéo :");

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

        if (signaler != null && MainController.recipientAddress != null) {
            signaler.sendCallEnd(MainController.recipientAddress, "VIDEO");
        } else {
            System.err.println("Erreur : signaler ou recipientAddress est nul.");
        }
        if (callDurationLabel != null) {
            callDurationLabel.setText("Durée : 00:00");
        } else {
            System.err.println("Erreur : callDurationLabel est nul.");
        }
      
        if (onHangUp != null) {
            Platform.runLater(onHangUp);
        }
    }
}
