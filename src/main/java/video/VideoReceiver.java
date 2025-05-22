package video;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

public class VideoReceiver extends Thread {
    private final int port;
    private  Consumer<BufferedImage> consumer;
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
                    BufferedImage img = ImageIO.read(in); // bloque si le flux n'est pas bien structuré
                    if (img != null) {
                        consumer.accept(img);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    break; // quitte si une erreur survient
                }
            }
        } catch (Exception e) {

            System.err.println("Attempting to bind on port: " + port);

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


