package video;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.OutputStream;
import java.net.Socket;


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
                //ce qui se passe pendant a l'envoi du flux
                while (!interrupted()) {
                    BufferedImage frame = camera.grabCurrentFrame();
                    if (frame != null) {
                        ImageIO.write(frame, "jpg", out);
                        out.flush(); // très important
                    }
                    Thread.sleep(100); // 10 FPS
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
