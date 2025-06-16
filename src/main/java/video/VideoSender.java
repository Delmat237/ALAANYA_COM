package video;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Cette classe gère l'envoi du flux vidéo via TCP à un hôte distant.
 */
public class VideoSender extends Thread {
    private final String host;
    private final int port;
    private final CameraService camera;
        private volatile boolean sending = false;

    private static final int TARGET_WIDTH = 640;
    private static final int TARGET_HEIGHT = 480;
    private volatile boolean running = true;

    public VideoSender(String host, int port, CameraService camera) {
        this.host = host;
        this.port = port;
        this.camera = camera;
    }

    @Override
    public void run() {
        try (Socket socket = new Socket(host, port);
             OutputStream out = socket.getOutputStream()) {

            System.out.println("📡 Connexion établie avec " + host + ":" + port);

            // Attend que la caméra démarre
            while (!camera.isStarted() && running) {
                Thread.sleep(50);
            }

            // Envoi continu tant que le thread est actif
            while (running && !isInterrupted()) {
                try {
                    BufferedImage original = camera.grabCurrentFrame();

                    if (original != null) {
                        BufferedImage resized = resizeImage(original, TARGET_WIDTH, TARGET_HEIGHT);

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(resized, "jpg", baos);
                        byte[] imageBytes = baos.toByteArray();

                        out.write(ByteBuffer.allocate(4).putInt(imageBytes.length).array());
                        out.write(imageBytes);
                        out.flush();
                    }

                    Thread.sleep(33); // ~30 FPS
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur lors de l'envoi du frame : " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Impossible de se connecter à " + host + ":" + port + " : " + e.getMessage());
        }

        System.out.println("📴 Fin de l'envoi vidéo.");
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
    }


    /**
     * Stoppe proprement le thread d’envoi.
     */
    public void stopSending() {
        running = false;
        this.interrupt();
    }



    // Call this when starting to send
    public void start() {
        sending = true;
        // existing start logic...
    }

    

    public boolean isSending() {
        return sending;
    }

    // existing fields and methods

}
