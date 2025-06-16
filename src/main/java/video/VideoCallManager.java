package video;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import signal.CallSignaler;
import controller.MainController;

public class VideoCallManager {
    // Instance singleton
    private static VideoCallManager instance;
    
    private VideoSender sender;
    private VideoReceiver receiver;
    private CameraService camera;
    private ImageView localView;
    private ImageView remoteView;
    private Label callDurationLabel;
    private Runnable onHangUp;
    private boolean isVideoEnabled = true;
    private final CallSignaler signaler = CallSignaler.getInstance();

    // Initialisation de l'instance
    public static void initialize(ImageView localView, ImageView remoteView,
                                Label callDurationLabel,
                                Runnable onCallAccepted, Runnable onHangUp) {
        instance = new VideoCallManager(localView, remoteView, callDurationLabel, onCallAccepted, onHangUp);
    }

    private VideoCallManager(ImageView localView, ImageView remoteView,
                           Label callDurationLabel,
                           Runnable onCallAccepted, Runnable onHangUp) {
        this.localView = localView;
        this.remoteView = remoteView;
        this.callDurationLabel = callDurationLabel;
        this.onHangUp = onHangUp;
    }

    // Méthodes statiques pour l'interface publique
    public static void startReceiving(int port) {
        if (instance == null) throw new IllegalStateException("VideoCallManager non initialisé");
        instance.startReceivingInstance(port);
    }

    public static boolean startSending(String ip, int port) {
        if (instance == null) throw new IllegalStateException("VideoCallManager non initialisé");
        return instance.startSendingInstance(ip, port);
    }

    public static void hangUp() {
        if (instance != null) {
            instance.hangUpInstance();
        }
    }

    // Implémentations d'instance
    private void startReceivingInstance(int port) {
        try {
            if (receiver == null || !receiver.isReceiving()) {
                receiver = new VideoReceiver(port, frame -> {
                    Image fxImage = SwingFXUtils.toFXImage(frame, null);
                    Platform.runLater(() -> {
                        if (remoteView != null) {
                            remoteView.setImage(fxImage);
                        }
                    });
                });
                receiver.start();
            }
        } catch (Exception e) {
            System.err.println("❌ Impossible de démarrer le récepteur vidéo : " + e.getMessage());
        }
    }

    private boolean startSendingInstance(String ip, int port) {
        try {
            if (camera == null || !camera.isRunning()) {
                camera = new CameraService(frame -> {
                    Image fxImage = SwingFXUtils.toFXImage(frame, null);
                    Platform.runLater(() -> {
                        if (localView != null && isVideoEnabled) {
                            localView.setImage(fxImage);
                        }
                    });
                });

                if (!camera.startCamera()) {
                    System.err.println("⚠️ Caméra non disponible. Envoi vidéo annulé.");
                    return false;
                }
            }

            if (sender == null || !sender.isSending()) {
                sender = new VideoSender(ip, port, camera);
                sender.start();
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage de l'envoi vidéo : " + e.getMessage());
        }
        return false;
    }

    private void hangUpInstance() {
        System.out.println("📞 Fin de l'appel vidéo...");

        if (sender != null) {
            sender.stopSending();
            sender = null;
        }

        if (receiver != null) {
            receiver.stopReceiving();
            receiver = null;
        }

        if (camera != null) {
            camera.stopCamera();
            camera = null;
        }

        if (signaler != null && MainController.recipientAddress != null) {
            signaler.sendCallEnd(MainController.recipientAddress, "VIDEO");
        }

        Platform.runLater(() -> {
            if (callDurationLabel != null) callDurationLabel.setText("Durée : 00:00");
            if (localView != null) localView.setImage(null);
            if (remoteView != null) remoteView.setImage(null);
            if (onHangUp != null) onHangUp.run();
        });
    }

    public void toggleVideo(boolean enable) {
       
        if (enable) {
            if (camera != null && !camera.isRunning()) {
                camera.startCamera();
            }
            if (sender != null && !sender.isSending()) {
                sender.start();
            }
            isVideoEnabled = true;
            // Code to enable video
            System.out.println("Video enabled");
        } else {
            if (camera != null) {
                camera.stopCamera();
            }
            if (sender != null) {
                sender.stopSending();
            }
            isVideoEnabled = false;
            // Code to disable video
            System.out.println("Video disabled");
        }
    
}
}