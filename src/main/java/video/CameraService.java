package video;

import org.bytedeco.javacv.*;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class CameraService extends Thread {
    private OpenCVFrameGrabber grabber;
    private final Consumer<BufferedImage> frameConsumer;
    private final Java2DFrameConverter converter;
    private volatile boolean running = true;
    private volatile boolean started = false;
    private final int cameraIndex;

    public CameraService(Consumer<BufferedImage> frameConsumer) {
        this(frameConsumer, 0); // Par défaut utilise la caméra index 0
    }

    public CameraService(Consumer<BufferedImage> frameConsumer, int cameraIndex) {
        this.frameConsumer = frameConsumer;
        this.converter = new Java2DFrameConverter();
        this.cameraIndex = cameraIndex;
    }

    @Override
    public void run() {
        grabber = new OpenCVFrameGrabber(cameraIndex);

        try {
            grabber.start();
            started = true;
            System.out.println("Caméra démarrée (index " + cameraIndex + ").");

            while (running) {
                Frame frame = grabber.grab();

                if (frame == null) {
                    System.err.println("⚠️ Frame nulle capturée !");
                    continue;
                }

                BufferedImage img = converter.convert(frame);
                if (img != null && frameConsumer != null) {
                    frameConsumer.accept(img);
                }

                Thread.sleep(33); // ~30 FPS
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du démarrage ou de la capture de la caméra :");
            e.printStackTrace();

        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur lors de la libération du grabber :");
                e.printStackTrace();
            }
            started = false;
            System.out.println("🎥 Caméra arrêtée.");
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void stopCapture() {
        running = false;
        if (started) {
            try {
                this.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public BufferedImage grabCurrentFrame() throws Exception {
        if (!started || grabber == null) {
            throw new IllegalStateException("⚠️ Grabber non démarré !");
        }
        Frame frame = grabber.grab();
        return (frame != null) ? converter.convert(frame) : null;
    }

}
