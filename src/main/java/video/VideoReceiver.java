package video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

public class VideoReceiver extends Thread {
    private final int port;
    private final Consumer<BufferedImage> consumer;
    private volatile boolean running = true;

    public VideoReceiver(int port, Consumer<BufferedImage> consumer) {
        this.port = port;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        if (running) {
            try (ServerSocket server = new ServerSocket(port)) {

                Socket client = server.accept();
                InputStream in = client.getInputStream();

                while (!interrupted()) {
                    BufferedImage img = ImageIO.read(in);
                    if (img != null) consumer.accept(img);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void stopReceiving() {
        running = false;
    }
}
