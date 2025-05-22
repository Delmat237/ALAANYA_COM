package video;

import org.bytedeco.javacv.*;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

public class CameraService {
    private OpenCVFrameGrabber grabber;
    private final Java2DFrameConverter converter = new Java2DFrameConverter();
    private final Consumer<BufferedImage> frameConsumer;
    private Thread captureThread;
    private volatile boolean running = false;
    private final int cameraIndex;

    public CameraService(Consumer<BufferedImage> frameConsumer) {
        this(frameConsumer, 0);
    }

    public CameraService(Consumer<BufferedImage> frameConsumer, int cameraIndex) {
        this.frameConsumer = frameConsumer;
        this.cameraIndex = cameraIndex;
    }

    /**
     * Tente de démarrer la caméra. Retourne true si succès, false sinon.
     */
    public boolean startCamera() {
        try {
            grabber = new OpenCVFrameGrabber(cameraIndex);
            grabber.start();
            running = true;

            captureThread = new Thread(this::captureLoop);
            captureThread.start();

            System.out.println("📸 Caméra démarrée (index " + cameraIndex + ")");
            return true;

        } catch (FrameGrabber.Exception e) {
            System.err.println("❌ Échec démarrage caméra (index " + cameraIndex + ") : " + e.getMessage());
            return false;
        }
    }

    private void captureLoop() {
        try {
            while (running) {
                Frame frame = grabber.grab();
                if (frame == null) {
                    System.err.println("⚠️ Frame nulle capturée !");
                    continue;
                }

                BufferedImage image = converter.convert(frame);
                if (image != null && frameConsumer != null) {
                    frameConsumer.accept(image);
                }

                Thread.sleep(33); // ~30 FPS
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur dans la capture vidéo : " + e.getMessage());
        } finally {
            stopCamera();
        }
    }
    public boolean isStarted() {
        return running;
    }


    /**
     * Stoppe la capture et libère les ressources.
     */
    public void stopCamera() {
        running = false;

        try {
            if (captureThread != null) {
                captureThread.join();
            }

            if (grabber != null) {
                grabber.stop();
                grabber.release();
                System.out.println("🎥 Caméra arrêtée.");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur arrêt caméra : " + e.getMessage());
        }
    }

    /**
     * Capture manuellement une image (instantané)
     */
    public BufferedImage grabCurrentFrame() throws Exception {
        if (!running || grabber == null) {
            throw new IllegalStateException("⚠️ Caméra non démarrée !");
        }
        Frame frame = grabber.grab();
        return (frame != null) ? converter.convert(frame) : null;
    }
}
