package video;

import javax.imageio.ImageIO;
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
                    BufferedImage frame = camera.grabCurrentFrame();

                    if (frame != null) {
                        // Convertit l'image en tableau de bytes
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(frame, "jpg", baos);
                        byte[] imageBytes = baos.toByteArray();

                        // Envoie la taille (4 octets) suivie des données de l'image
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

    /**
     * Stoppe proprement le thread d’envoi.
     */
    public void stopSending() {
        running = false;
        this.interrupt();
    }
}
