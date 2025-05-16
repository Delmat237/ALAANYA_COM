package video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;


@SuppressWarnings("printStackTrace")
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
        if(running) {
            try (Socket socket = new Socket(host, port);

        
                 OutputStream out = socket.getOutputStream()) {
                    //attend que la camera demarre
            while (!camera.isStarted()) {
                Thread.sleep(50);
            }
                //ce qui se passe pendant a l'envoi du flux
            while (!interrupted()) {
            BufferedImage frame = camera.grabCurrentFrame();
            if (frame != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(frame, "jpg", baos);
                byte[] imageBytes = baos.toByteArray();

                // Envoyer la taille
                out.write(ByteBuffer.allocate(4).putInt(imageBytes.length).array());
                // Envoyer l'image
                out.write(imageBytes);
                out.flush();
            }
            Thread.sleep(100);
        }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void stopSending() {
        running = false;
    }

}
