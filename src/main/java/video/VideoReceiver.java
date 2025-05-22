package video;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

public class VideoReceiver extends Thread {
    private final int port;
    private Consumer<BufferedImage> consumer;
    private volatile boolean running = true;
    private Socket client;

    public VideoReceiver(int port, Consumer<BufferedImage> consumer) {
        this.port = port;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        try (ServerSocket server = new ServerSocket(port)) {
            client = server.accept();
            InputStream in = client.getInputStream();

            while (running && !interrupted()) {
                try {
                    // Lire la taille de l'image (4 octets)
                    byte[] sizeBytes = in.readNBytes(4);
                    if (sizeBytes.length < 4) break;

                    int size = ByteBuffer.wrap(sizeBytes).getInt();

                    // Lire l'image elle-même
                    byte[] imageBytes = in.readNBytes(size);
                    if (imageBytes.length < size) break;

                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (img != null) {
                        consumer.accept(img);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Erreur sur VideoReceiver : " + e.getMessage());
        }
    }

    public void stopReceiving() {
        running = false;
        this.interrupt();
        try {
            if (client != null && !client.isClosed()) {
                client.close(); // Ferme la socket pour débloquer le InputStream
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
