package video;

import java.awt.*;
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


    private static final int TARGET_WIDTH = 640;
    private static final int TARGET_HEIGHT = 480;

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
                    byte[] sizeBytes = in.readNBytes(4);
                    if (sizeBytes.length < 4) break;

                    int size = ByteBuffer.wrap(sizeBytes).getInt();
                    byte[] imageBytes = in.readNBytes(size);
                    if (imageBytes.length < size) break;

                    BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
                    if (original != null) {
                        BufferedImage resized = resizeImage(original, TARGET_WIDTH, TARGET_HEIGHT);
                        consumer.accept(resized);
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

    // 🔧 Méthode de redimensionnement
    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, width, height, null);
        g.dispose();
        return resized;
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
